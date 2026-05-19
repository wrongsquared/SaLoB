import { test, expect } from "@playwright/test"

test.describe("SaLoB smoke tests", () => {
  test("homepage loads and shows map UI controls", async ({ page }) => {
    await page.goto("/")

    const modeToggle = page.getByRole("radiogroup", { name: "Map mode" })
    await expect(modeToggle).toBeVisible()
    await expect(page.getByRole("radio", { name: "Eatery" })).toBeVisible()
    await expect(page.getByRole("radio", { name: "Food" })).toBeVisible()

    const searchBar = page.getByPlaceholder("Search eateries...")
    await expect(searchBar).toBeVisible()

    const fab = page.getByLabel("Submit price")
    await expect(fab).toBeVisible()

    const leafletMap = page.locator(".leaflet-container")
    await expect(leafletMap).toBeVisible()
  })

  test("mode toggle switches between eatery and food views", async ({ page }) => {
    await page.goto("/")
    await page.waitForTimeout(500)

    await page.getByRole("radio", { name: "Food" }).click({ force: true })
    await expect(page.getByPlaceholder("Search foods to show on map... (max 5)")).toBeVisible({ timeout: 5000 })

    await page.getByRole("radio", { name: "Eatery" }).click({ force: true })
    await expect(page.getByPlaceholder("Search eateries...")).toBeVisible({ timeout: 5000 })
  })

  test("login page loads with branding", async ({ page }) => {
    await page.goto("/login")
    await expect(page.getByText("Keep Tabs on the Price of Living")).toBeVisible()
  })

  test("navigation links are present", async ({ page }) => {
    await page.goto("/")
    await expect(page.getByRole("link", { name: "Dashboard" })).toBeVisible()
    await expect(page.getByRole("link", { name: "Analytics" })).toBeVisible()
    await expect(page.getByRole("link", { name: "Reports" })).toBeVisible()
  })

  test("markers render on the map from bounds API", async ({ page }) => {
    await page.goto("/")

    await page.waitForResponse((response) =>
      response.url().includes("/api/eateries/within-bounds") && response.status() === 200,
    )

    const leafletMap = page.locator(".leaflet-container")
    const markers = leafletMap.locator(".leaflet-marker-icon")
    await expect(markers.first()).toBeVisible({ timeout: 5000 })
  })

  test("clicking a marker opens eatery sidebar", async ({ page }) => {
    await page.goto("/")

    await page.waitForResponse((response) =>
      response.url().includes("/api/eateries/within-bounds") && response.status() === 200,
    )

    const firstMarker = page.locator(".leaflet-marker-icon").first()
    await expect(firstMarker).toBeVisible({ timeout: 5000 })
    await firstMarker.click()

    const sidebar = page.getByRole("dialog", { name: "Eatery details" })
    await expect(sidebar).toBeVisible({ timeout: 5000 })
  })

  test("submission wizard opens from FAB", async ({ page }) => {
    await page.goto("/")

    await page.getByLabel("Submit price").click()

    await expect(page.getByText("Select an eatery")).toBeVisible()
  })
})
