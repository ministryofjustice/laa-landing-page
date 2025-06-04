const pa11y = require('pa11y');
const puppeteer = require('puppeteer');

(async () => {
  const namespace = process.env.NAMESPACE;
  if (!namespace) {
    console.error("NAMESPACE environment variable is not set.");
    process.exit(1);
  }

  const loginUrl = `https://${namespace}.apps.live.cloud-platform.service.justice.gov.uk`;
  console.log(`Navigating to: ${loginUrl}`);

  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();

  // Optional: Simulate login here if needed
  await page.goto(loginUrl);
  await page.waitForTimeout(3000); // wait for any redirects or JS auth
  console.log("Page loaded.");

  // Run Pa11y
  const results = await pa11y(loginUrl, {
    browser,
    page,
    runner: 'puppeteer',
    timeout: 30000,
  });

  console.log("Pa11y Results:");
  console.log(results);

  await browser.close();
})();

