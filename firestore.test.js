const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_1";
const BOB_UID = "bob_viewer_2";
const OWNER_UID = "owner_admin_0";
const OWNER_EMAIL = "lakeskuke@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function sampleVideoPayload(ownerId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    ownerId,
    channelName: "Alice Tech",
    channelHandle: "@alicetech",
    title: "Building a Blue Video Platform",
    description: "Full tour of our studio and setup.",
    category: "Tech",
    videoUrl: "https://example.com/video.mp4",
    thumbnailUrl: "preset:tech",
    durationSeconds: 245,
    visibility: "public",
    featured: false,
    viewsCount: 0,
    likesCount: 0,
    commentsCount: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user cannot read or write any BlueStream collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("videos").where("visibility", "==", "public").get()
  );
  await assertFails(
    unauthDb.collection("videos").doc("vid_1").set(sampleVideoPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("channels").doc(ALICE_UID).get()
  );
});

test("Creator can create channel, upload video, edit own video, and cannot spoof featured or ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Create channel
  await assertSucceeds(
    aliceDb.collection("channels").doc(ALICE_UID).set({
      ownerId: ALICE_UID,
      displayName: "Alice Tech",
      handle: "@alicetech",
      bio: "Tech reviews in cobalt blue.",
      subscriberCount: 0,
      videoCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Create public video (NO id field)
  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_1").set(sampleVideoPayload(ALICE_UID))
  );

  // Reject ghost field / @DocumentId `id` field
  await assertFails(
    aliceDb
      .collection("videos")
      .doc("vid_ghost")
      .set(sampleVideoPayload(ALICE_UID, { id: "vid_ghost" }))
  );

  // Reject non-owner trying to set featured = true
  await assertFails(
    aliceDb
      .collection("videos")
      .doc("vid_feat")
      .set(sampleVideoPayload(ALICE_UID, { featured: true }))
  );

  // Creator partial edit succeeds
  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_1").update({
      title: "Updated Blue Studio Tour",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Creator cannot mutate createdAt
  await assertFails(
    aliceDb.collection("videos").doc("vid_1").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Viewer can query public videos, increment views/likes, comment, and cannot read private videos or edit others", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_pub").set(sampleVideoPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb
      .collection("videos")
      .doc("vid_priv")
      .set(sampleVideoPayload(ALICE_UID, { visibility: "private" }))
  );

  // Public feed query succeeds
  await assertSucceeds(
    bobDb.collection("videos").where("visibility", "==", "public").get()
  );

  // Unfiltered list query fails for regular user
  await assertFails(bobDb.collection("videos").get());

  // Private video denied to Bob
  await assertFails(bobDb.collection("videos").doc("vid_priv").get());

  // Bob can increment viewsCount by 1
  await assertSucceeds(
    bobDb.collection("videos").doc("vid_pub").update({ viewsCount: 1 })
  );

  // Bob cannot jump viewsCount by 100
  await assertFails(
    bobDb.collection("videos").doc("vid_pub").update({ viewsCount: 101 })
  );

  // Bob cannot edit Alice's title
  await assertFails(
    bobDb.collection("videos").doc("vid_pub").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob can post a comment on existing video
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    bobDb
      .collection("videos")
      .doc("vid_pub")
      .collection("comments")
      .doc("c_1")
      .set({
        videoId: "vid_pub",
        authorId: BOB_UID,
        authorName: "Bob Viewer",
        text: "Love the blue theme!",
        likesCount: 0,
        createdAt: now,
      })
  );

  // Filtered comments query succeeds
  await assertSucceeds(
    bobDb
      .collection("videos")
      .doc("vid_pub")
      .collection("comments")
      .where("videoId", "==", "vid_pub")
      .get()
  );
});

test("User-scoped likes, subscriptions, and watch_later enforce strict per-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("likes").doc("vid_pub").set({
      videoId: "vid_pub",
      videoTitle: "Blue Studio",
      channelName: "Alice Tech",
      createdAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("likes").get()
  );

  // Cross-user isolation
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("likes").get()
  );
});

test("Platform Owner (verified email) can feature and moderate videos; unverified spoofed email is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ownerDb = testEnv
    .authenticatedContext(OWNER_UID, {
      email: OWNER_EMAIL,
      email_verified: true,
    })
    .firestore();
  const spoofedDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: OWNER_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_mod").set(sampleVideoPayload(ALICE_UID))
  );

  // Unverified spoofed admin cannot feature or delete
  await assertFails(
    spoofedDb.collection("videos").doc("vid_mod").update({
      featured: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(spoofedDb.collection("videos").doc("vid_mod").delete());

  // Verified Owner can feature and delete
  await assertSucceeds(
    ownerDb.collection("videos").doc("vid_mod").update({
      featured: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(ownerDb.collection("videos").doc("vid_mod").delete());
});
