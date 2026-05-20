import { http, HttpResponse, delay } from "msw"

const MOCK_USER_ID = "550e8400-e29b-41d4-a716-446655440000"

const EATERIES_MAP = [
  { eateryId: "a1b2c3d4-e29b-41d4-a716-446655440001", name: "Tian Tian Hainanese Chicken Rice", latitude: 1.2835, longitude: 103.8437, typeLabel: "Hawker Stall" },
  { eateryId: "a1b2c3d4-e29b-41d4-a716-446655440002", name: "328 Katong Laksa", latitude: 1.3005, longitude: 103.9032, typeLabel: "Hawker Stall" },
  { eateryId: "a1b2c3d4-e29b-41d4-a716-446655440003", name: "Ya Kun Kaya Toast", latitude: 1.2850, longitude: 103.8510, typeLabel: "Cafe" },
  { eateryId: "a1b2c3d4-e29b-41d4-a716-446655440004", name: "Newton Food Centre", latitude: 1.3115, longitude: 103.8375, typeLabel: "Hawker Centre" },
  { eateryId: "a1b2c3d4-e29b-41d4-a716-446655440005", name: "Maxwell Food Centre", latitude: 1.2795, longitude: 103.8440, typeLabel: "Hawker Centre" },
]

const FOOD_PREVIEWS = [
  { foodEntryId: "b2c3d4e5-e29b-41d4-a716-446655440001", name: "Chicken Rice", sgCents: 450, upvotes: 23, downvotes: 2, photoPresignedUrl: "/mock/chicken-rice.jpg", submitterId: MOCK_USER_ID, submitterUsername: "foodie_alan", createdAt: "2026-05-01T10:00:00Z" },
  { foodEntryId: "b2c3d4e5-e29b-41d4-a716-446655440002", name: "Laksa", sgCents: 680, upvotes: 18, downvotes: 1, photoPresignedUrl: "/mock/laksa.jpg", submitterId: MOCK_USER_ID, submitterUsername: "spice_hunter", createdAt: "2026-04-28T12:30:00Z" },
  { foodEntryId: "b2c3d4e5-e29b-41d4-a716-446655440003", name: "Kaya Toast Set", sgCents: 320, upvotes: 45, downvotes: 3, photoPresignedUrl: "/mock/kaya-toast.jpg", submitterId: MOCK_USER_ID, submitterUsername: "breakfast_club", createdAt: "2026-05-15T08:00:00Z" },
  { foodEntryId: "b2c3d4e5-e29b-41d4-a716-446655440004", name: "Satay (10 sticks)", sgCents: 800, upvotes: 31, downvotes: 4, photoPresignedUrl: "/mock/satay.jpg", submitterId: MOCK_USER_ID, submitterUsername: "grill_master", createdAt: "2026-05-10T19:00:00Z" },
  { foodEntryId: "b2c3d4e5-e29b-41d4-a716-446655440005", name: "Fishball Noodles", sgCents: 380, upvotes: 12, downvotes: 0, photoPresignedUrl: "/mock/fishball-noodles.jpg", submitterId: MOCK_USER_ID, submitterUsername: "noodle_whisperer", createdAt: "2026-05-12T11:00:00Z" },
]

const FOOD_DB = [
  { foodId: "c3d4e5f6-e29b-41d4-a716-446655440001", foodName: "Chicken Rice", photoUrl: "/mock/chicken-rice.jpg" },
  { foodId: "c3d4e5f6-e29b-41d4-a716-446655440002", foodName: "Laksa", photoUrl: "/mock/laksa.jpg" },
  { foodId: "c3d4e5f6-e29b-41d4-a716-446655440003", foodName: "Kaya Toast Set", photoUrl: "/mock/kaya-toast.jpg" },
  { foodId: "c3d4e5f6-e29b-41d4-a716-446655440004", foodName: "Satay (10 sticks)", photoUrl: "/mock/satay.jpg" },
  { foodId: "c3d4e5f6-e29b-41d4-a716-446655440005", foodName: "Fishball Noodles", photoUrl: "/mock/fishball-noodles.jpg" },
]

const reportedEateries = new Set<string>()
let nextFoodId = 6

export const handlers = [
  http.get("/api/eateries/within-bounds", async ({ request }) => {
    await delay(50)
    const url = new URL(request.url)
    const minLat = parseFloat(url.searchParams.get("minLat") ?? "0")
    const maxLat = parseFloat(url.searchParams.get("maxLat") ?? "0")
    const minLon = parseFloat(url.searchParams.get("minLon") ?? "0")
    const maxLon = parseFloat(url.searchParams.get("maxLon") ?? "0")
    if (minLat >= maxLat || minLon >= maxLon) {
      return HttpResponse.json({ error: "Invalid bounds" }, { status: 400 })
    }
    const filtered = EATERIES_MAP.filter(
      (e) => e.latitude >= minLat && e.latitude <= maxLat && e.longitude >= minLon && e.longitude <= maxLon,
    )
    return HttpResponse.json(filtered)
  }),

  http.get("/api/eateries/search", async ({ request }) => {
    await delay(30)
    const url = new URL(request.url)
    const search = url.searchParams.get("search")?.toLowerCase() ?? ""
    const results = EATERIES_MAP
      .filter((e) => e.name.toLowerCase().includes(search))
      .map((e) => ({ eateryId: e.eateryId, name: e.name, address: "1 Example St, Singapore" }))
    if (results.length === 0) return new HttpResponse(null, { status: 204 })
    return HttpResponse.json(results)
  }),

  http.get("/api/eateries/:eateryId", async ({ params }) => {
    await delay(80)
    const eatery = EATERIES_MAP.find((e) => e.eateryId === params.eateryId)
    if (!eatery) return HttpResponse.json({ error: "Not found" }, { status: 404 })
    return HttpResponse.json({
      eateryId: eatery.eateryId,
      name: eatery.name,
      address: "1 Example St, Singapore",
      typeLabel: eatery.typeLabel,
      photoUrl: `/mock/eatery-${eatery.eateryId}.jpg`,
      foodPreviews: FOOD_PREVIEWS.slice(0, 3),
    })
  }),

  http.get("/api/foods/search", async ({ request }) => {
    await delay(30)
    const url = new URL(request.url)
    const search = url.searchParams.get("search")?.toLowerCase() ?? ""
    const results = FOOD_PREVIEWS
      .filter((f) => f.name.toLowerCase().includes(search))
      .map((f) => ({ foodId: f.foodEntryId, foodName: f.name, photoUrl: f.photoPresignedUrl }))
    return HttpResponse.json(results)
  }),

  http.get("/api/food-entries/historical-data/:foodEntryId", async ({ params, request }) => {
    await delay(100)
    const url = new URL(request.url)
    if (!url.searchParams.has("startDate")) {
      return HttpResponse.json({ error: "startDate query param is required" }, { status: 400 })
    }
    const entry = FOOD_PREVIEWS.find((f) => f.foodEntryId === params.foodEntryId)
    if (!entry) return HttpResponse.json({ error: "Not found" }, { status: 404 })
    return HttpResponse.json({
      foodName: entry.name,
      sgCentsConsensusPrice: entry.sgCents,
      eateryId: "a1b2c3d4-e29b-41d4-a716-446655440001",
      eateryAddress: "1 Example St, Singapore",
      submitterUsername: entry.submitterUsername,
      availableDates: ["2026-05-01", "2026-04-28", "2026-04-15"],
      benchmarkDateEntries: [entry],
      consensusEntry: {
        foodEntryId: entry.foodEntryId,
        foodPhotoPresignedUrl: entry.photoPresignedUrl,
        submittedAt: entry.createdAt,
        submitterId: entry.submitterId,
        submitterUsername: entry.submitterUsername,
        submitterProfilePhotoPresignedUrl: "/mock/avatar.jpg",
        submitterWtfScore: 72.5,
        submitterTenureDays: 180,
        submitterEntriesSubmitted: 34,
      },
    })
  }),

  http.get("/api/food-entries/:foodEntryId/details", async ({ params }) => {
    await delay(60)
    const entry = FOOD_PREVIEWS.find((f) => f.foodEntryId === params.foodEntryId)
    if (!entry) return HttpResponse.json({ error: "Not found" }, { status: 404 })
    return HttpResponse.json({
      foodEntryId: entry.foodEntryId,
      foodPhotoPresignedUrl: entry.photoPresignedUrl,
      submittedAt: entry.createdAt,
      submitterId: entry.submitterId,
      submitterUsername: entry.submitterUsername,
      submitterProfilePhotoPresignedUrl: "/mock/avatar.jpg",
      submitterWtfScore: 72.5,
      submitterTenureDays: 180,
      submitterEntriesSubmitted: 34,
    })
  }),

  http.post("/api/food-entries/submit", async () => {
    await delay(100)
    return new HttpResponse(null, { status: 200 })
  }),

  http.get("/api/food-entries/within-bounds", async ({ request }) => {
    await delay(50)
    const url = new URL(request.url)
    const minLat = parseFloat(url.searchParams.get("minLat") ?? "0")
    const maxLat = parseFloat(url.searchParams.get("maxLat") ?? "0")
    const minLon = parseFloat(url.searchParams.get("minLon") ?? "0")
    const maxLon = parseFloat(url.searchParams.get("maxLon") ?? "0")
    if (minLat >= maxLat || minLon >= maxLon) {
      return HttpResponse.json({ error: "Invalid bounds" }, { status: 400 })
    }
    const filtered = EATERIES_MAP.filter(
      (e) => e.latitude >= minLat && e.latitude <= maxLat && e.longitude >= minLon && e.longitude <= maxLon,
    )
    const foodEntries = filtered.flatMap((eatery, eateryIdx) =>
      FOOD_PREVIEWS.slice(0, 3).map((fp, fpIdx) => ({
        foodEntryId: fp.foodEntryId,
        foodName: fp.name,
        sgCents: fp.sgCents,
        eateryId: eatery.eateryId,
        eateryName: eatery.name,
        latitude: eatery.latitude + (fpIdx * 0.0001),
        longitude: eatery.longitude + (eateryIdx * 0.0001),
      })),
    )
    return HttpResponse.json(foodEntries)
  }),

  http.post("/api/foods", async ({ request }) => {
    await delay(80)
    const body = (await request.json()) as { foodName: string }
    const existing = FOOD_DB.find((f) => f.foodName.toLowerCase() === body.foodName.toLowerCase())
    if (existing) {
      return HttpResponse.json(existing)
    }
    const newId = `c3d4e5f6-e29b-41d4-a716-44665544000${nextFoodId++}`
    const newFood = { foodId: newId, foodName: body.foodName, photoUrl: "" }
    FOOD_DB.push(newFood)
    return HttpResponse.json(newFood)
  }),

  http.post("/api/eateries/:eateryId/report-closed", async ({ params }) => {
    await delay(50)
    const key = params.eateryId as string
    if (reportedEateries.has(key)) {
      return HttpResponse.json({ error: "Already reported" }, { status: 409 })
    }
    reportedEateries.add(key)
    return new HttpResponse(null, { status: 204 })
  }),

  http.post("/api/auth/login", async ({ request }) => {
    await delay(150)
    const body = (await request.json()) as { usernameOrEmail?: string; password?: string }
    if (!body.usernameOrEmail || !body.password) {
      return HttpResponse.json({ error: "Missing credentials" }, { status: 400 })
    }
    return HttpResponse.json({ jwt: "mock-jwt-token-for-testing" })
  }),

  http.post("/api/auth/register", async () => {
    await delay(200)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get("/api/users/me", async () => {
    await delay(50)
    return HttpResponse.json({
      id: MOCK_USER_ID,
      email: "testuser@salob.com",
      username: "testuser",
      roles: ["CONTRIBUTOR"],
      avatarUrl: "/mock/avatar.jpg",
    })
  }),
]
