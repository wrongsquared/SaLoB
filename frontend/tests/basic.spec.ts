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

  test("eatery panel shows Report as Closed button", async ({ page }) => {
    await page.goto("/")

    await page.waitForResponse((response) =>
      response.url().includes("/api/eateries/within-bounds") && response.status() === 200,
    )

    const firstMarker = page.locator(".leaflet-marker-icon").first()
    await firstMarker.click()

    const sidebar = page.getByRole("dialog", { name: "Eatery details" })
    await expect(sidebar).toBeVisible({ timeout: 5000 })
    await expect(page.getByLabel("Report as closed")).toBeVisible()
  })

  test("food mode markers render from food entries API", async ({ page }) => {
    await page.goto("/")

    await page.getByRole("radio", { name: "Food" }).click({ force: true })

    await page.waitForResponse((response) =>
      response.url().includes("/api/food-entries/within-bounds") && response.status() === 200,
    )

    const leafletMap = page.locator(".leaflet-container")
    const markers = leafletMap.locator(".leaflet-marker-icon")
    await expect(markers.first()).toBeVisible({ timeout: 5000 })
  })

  test("submission wizard full flow — eatery, food, price, confirm", async ({ page }) => {
    await page.goto("/")

    await page.getByLabel("Submit price").click()
    await expect(page.getByText("Select an eatery")).toBeVisible()

    const searchInput = page.getByRole("dialog", { name: "Submission wizard" }).getByPlaceholder("Search eateries...")
    await searchInput.fill("Tian")
    await page.waitForTimeout(400)
    await page.getByText("Tian Tian Hainanese Chicken Rice").click()

    await expect(page.getByText("Select a food")).toBeVisible()
    const foodSearch = page.getByPlaceholder("Search foods...")
    await foodSearch.fill("Chicken")
    await page.waitForTimeout(400)
    await page.getByText("Chicken Rice").click()

    await expect(page.getByText("Enter the price")).toBeVisible()
    const priceInput = page.getByPlaceholder("$0.00")
    await priceInput.fill("5.50")
    await page.getByText(/Continue.*\$5\.50/).click()

    await expect(page.getByText("Confirm submission")).toBeVisible()
    await expect(page.getByText("Tian Tian Hainanese Chicken Rice")).toBeVisible()
    await expect(page.getByText("Chicken Rice", { exact: true })).toBeVisible()
    await expect(page.getByText("$5.50")).toBeVisible()

    await page.getByRole("dialog", { name: "Submission wizard" }).getByRole("button", { name: "Submit" }).click()
    await expect(page.getByText("Price submitted!")).toBeVisible({ timeout: 5000 })
  })

  test.describe("visual smoke tests", () => {
    test("homepage default state screenshot", async ({ page }) => {
      await page.goto("/")
      await page.waitForResponse((response) =>
        response.url().includes("/api/eateries/within-bounds") && response.status() === 200,
      )
      await page.waitForTimeout(1000)
      await expect(page).toHaveScreenshot("homepage-default.png", { maxDiffPixels: 1000 })
    })

    test("eatery sidebar open screenshot", async ({ page }) => {
      await page.goto("/")
      await page.waitForResponse((response) =>
        response.url().includes("/api/eateries/within-bounds") && response.status() === 200,
      )
      const firstMarker = page.locator(".leaflet-marker-icon").first()
      await expect(firstMarker).toBeVisible({ timeout: 5000 })
      await firstMarker.click()
      await page.waitForTimeout(500)
      await expect(page).toHaveScreenshot("homepage-sidebar-open.png", { maxDiffPixels: 1000 })
    })

    test("food mode screenshot", async ({ page }) => {
      await page.goto("/")
      await page.getByRole("radio", { name: "Food" }).click({ force: true })
      await page.waitForResponse((response) =>
        response.url().includes("/api/food-entries/within-bounds") && response.status() === 200,
      )
      await page.waitForTimeout(1000)
      await expect(page).toHaveScreenshot("homepage-food-mode.png", { maxDiffPixels: 1000 })
    })
  })
})
