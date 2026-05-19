import { test, expect } from "@playwright/test"

test.describe("SaLoB smoke tests", () => {
  test("homepage loads and shows the map view heading", async ({ page }) => {
    await page.goto("/")
    await expect(page.locator("h1")).toBeVisible()
  })

  test("login page loads with branding", async ({ page }) => {
    await page.goto("/login")
    await expect(page.getByText("Keep Tabs on the Price of Living")).toBeVisible()
  })

  test("navigation links are present", async ({ page }) => {
    await page.goto("/")
    await expect(page.getByRole("link", { name: "Dashboard" })).toBeVisible()
    await expect(page.getByRole("link", { name: "Map View" })).toBeVisible()
    await expect(page.getByRole("link", { name: "Analytics" })).toBeVisible()
    await expect(page.getByRole("link", { name: "Reports" })).toBeVisible()
  })
})
