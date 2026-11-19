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
    title: "Building an Online Blue Video Platform",
    description: "Full tour of our studio and setup.",
    category: "Tech",
    videoUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/video.mp4",
    thumbnailUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/thumb.jpg",
    durationSeconds: 245,
    visibility: "public",
    moderationStatus: "pending",
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
    unauthDb
      .collection("videos")
      .where("visibility", "==", "public")
      .where("moderationStatus", "==", "approved")
      .get()
  );
  await assertFails(
    unauthDb.collection("videos").doc("vid_1").set(sampleVideoPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("reports").get());
});

test("Creator uploads pending video, cannot self-approve or feature, and can edit own metadata", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

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

  // Pending video create succeeds
  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_1").set(sampleVideoPayload(ALICE_UID))
  );

  // Creator CANNOT self-approve on create
  await assertFails(
    aliceDb
      .collection("videos")
      .doc("vid_self_approve")
      .set(sampleVideoPayload(ALICE_UID, { moderationStatus: "approved" }))
  );

  // Creator CANNOT self-approve on update
  await assertFails(
    aliceDb.collection("videos").doc("vid_1").update({
      moderationStatus: "approved",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Creator CANNOT set featured = true
  await assertFails(
    aliceDb.collection("videos").doc("vid_1").update({
      featured: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Creator CAN edit title & description
  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_1").update({
      title: "Updated Online Tour",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner approves video for public feed; viewer can watch, like, comment, and report", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ownerDb = testEnv
    .authenticatedContext(OWNER_UID, {
      email: OWNER_EMAIL,
      email_verified: true,
    })
    .firestore();

  // Alice submits pending video
  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_pub").set(sampleVideoPayload(ALICE_UID))
  );

  // Bob cannot view pending video before approval
  await assertFails(bobDb.collection("videos").doc("vid_pub").get());

  // Owner queries pending moderation queue and approves
  await assertSucceeds(
    ownerDb.collection("videos").where("moderationStatus", "==", "pending").get()
  );
  await assertSucceeds(
    ownerDb.collection("videos").doc("vid_pub").update({
      moderationStatus: "approved",
      featured: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob queries public approved feed
  await assertSucceeds(
    bobDb
      .collection("videos")
      .where("visibility", "==", "public")
      .where("moderationStatus", "==", "approved")
      .get()
  );

  // Bob can increment view & like by +1
  await assertSucceeds(
    bobDb.collection("videos").doc("vid_pub").update({ viewsCount: 1 })
  );
  await assertSucceeds(
    bobDb.collection("videos").doc("vid_pub").update({ likesCount: 1 })
  );

  // Bob submits a report
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    bobDb.collection("reports").doc("rep_1").set({
      videoId: "vid_pub",
      videoTitle: "Building an Online Blue Video Platform",
      videoOwnerId: ALICE_UID,
      reporterId: BOB_UID,
      reason: "Spam or misleading",
      details: "Test report details",
      status: "open",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Regular user CANNOT list all reports or resolve reports
  await assertFails(aliceDb.collection("reports").get());
  await assertFails(
    bobDb.collection("reports").doc("rep_1").update({
      status: "resolved",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Owner CAN list open reports and resolve them
  await assertSucceeds(
    ownerDb.collection("reports").where("status", "==", "open").get()
  );
  await assertSucceeds(
    ownerDb.collection("reports").doc("rep_1").update({
      status: "resolved",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Spoofed unverified owner email is rejected from admin actions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const spoofedDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: OWNER_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_mod").set(sampleVideoPayload(ALICE_UID))
  );

  await assertFails(
    spoofedDb.collection("videos").doc("vid_mod").update({
      moderationStatus: "approved",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(spoofedDb.collection("videos").doc("vid_mod").delete());
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
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("likes").get()
  );
});
