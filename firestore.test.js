const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read notes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notes").get());
});

test("Unauthenticated user: cannot write notes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Test Note",
      content: "Markdown body",
      emoji: "📝",
      colorHex: "#202124",
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      tags: "test",
      folder: "Vault",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user Alice: can create note in her own vault", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Alice's Note",
      content: "## Heading\n- [ ] Task 1",
      emoji: "📝",
      colorHex: "#202124",
      bannerColor: "#6366F1",
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      tags: "work,test",
      folder: "Vault",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user Bob: cannot read Alice's note", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Alice Secret",
      content: "Top secret",
      emoji: "🔒",
      colorHex: "#202124",
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      tags: "secret",
      folder: "Vault",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").get());
});

test("Authenticated user Bob: cannot write into Alice's vault", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("bob_hack").set({
      userId: BOB_UID,
      title: "Hacked",
      content: "Hacked",
      emoji: "💀",
      colorHex: "#202124",
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      tags: "",
      folder: "Vault",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user Alice: cannot create note with missing required fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_bad").set({
      userId: ALICE_UID,
      title: "Missing fields",
      // missing content, emoji, etc.
    })
  );
});
