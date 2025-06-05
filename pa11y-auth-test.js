const pa11y = require('pa11y');
const puppeteer = require('puppeteer');

(async () => {
  const namespace = process.env.NAMESPACE;
  const username = process.env.ENTRA_USERNAME;
  const password = process.env.ENTRA_PASSWORD;

  console.log('NAMESPACE:', process.env.NAMESPACE ? '[SET]' : '[MISSING]');
  console.log('ENTRA_USERNAME:', process.env.ENTRA_USERNAME ? '[SET]' : '[MISSING]');
  console.log('ENTRA_PASSWORD:', process.env.ENTRA_PASSWORD ? '[SET]' : '[MISSING]');
  
  ['NAMESPACE', 'ENTRA_USERNAME', 'ENTRA_PASSWORD'].forEach((key) => {
    if (!process.env[key]) {
      console.error(`Missing required env var: ${key}`);
    }
    });

  if (!namespace || !username || !password) {
    console.error("One or more required environment variables (NAMESPACE, ENTRA_USERNAME, ENTRA_PASSWORD) are missing.");
    process.exit(1);
  }

  const urls = [
    `https://${namespace}.apps.live.cloud-platform.service.justice.gov.uk`,
    `https://${namespace}.apps.live.cloud-platform.service.justice.gov.uk`,
  ];

  const browser = await puppeteer.launch({
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  const page = await browser.newPage();
  try {
    console.log('Step 1: Navigating to login page...');
    await page.goto(`https://${namespace}.apps.live.cloud-platform.service.justice.gov.uk`, { waitUntil: 'networkidle2' });

    // === FIRST LOGIN ATTEMPT ===
    console.log('Step 2: Typing username...');
    await page.waitForSelector('#email', { visible: true });
    await page.type('#email', username);

    console.log('Step 3: Clicking email "Sign in"...');
    await page.waitForSelector('button[type="submit"]', { visible: true });
    await page.click('button[type="submit"]');
    await page.waitForNavigation({ waitUntil: 'networkidle2' });

    console.log('Step 4: Typing password...');
    await page.waitForSelector('input[type="password"]', { visible: true });
    await page.type('input[type="password"]', password);

    console.log('Step 5: Clicking password "Sign in"...');
    await page.waitForSelector('button[type="submit"]', { visible: true });
    await page.click('button[type="submit"]');
    await page.waitForNavigation({ waitUntil: 'networkidle2' });

    // === OPTIONAL SECOND LOGIN ===
    try {
      console.log('Step 6: Checking for second login screen...');
      await page.waitForSelector('#email', { visible: true, timeout: 5000 });

      console.log('Second login detected. Re-entering credentials...');
      await page.type('#email', username);
      await page.click('button[type="submit"]');
      await page.waitForNavigation({ waitUntil: 'networkidle2' });
      await page.waitForSelector('input[type="password"]', { visible: true });
      await page.type('input[type="password"]', password);
      await page.click('button[type="submit"]');
      await page.waitForNavigation({ waitUntil: 'networkidle2' });

    } catch (secondLoginSkip) {
      console.log('No second login screen detected. Continuing...');
    }

    // === OPTIONAL "STAY SIGNED IN?" SCREEN ===
    try {
      console.log('Step 7: Checking for "Stay signed in?" prompt...');
      await page.waitForSelector('#idBtn_Back', { timeout: 5000 });

      console.log('"Stay signed in?" prompt detected. Clicking "No"...');
      await page.click('#idBtn_Back');
      await page.waitForNavigation({ waitUntil: 'networkidle2' });
    } catch (staySignedInSkip) {
      console.log('No "Stay signed in?" prompt appeared.');
    }

    console.log('🎉 Login flow completed successfully');

    // === OPTIONAL: Run Pa11y Accessibility Test ===
    const results = await pa11y(`https://${namespace}.apps.live.cloud-platform.service.justice.gov.uk`, {
      browser,
      page,
    });

    console.log('📊 Accessibility results:');
    console.log(results);

  
  } catch (error) {
    console.error('Login automation failed:', error);
    await page.screenshot({ path: 'login-failure.png' });
    process.exit(1);
  }  
  

  // Loop through URLs after login
  for (const url of urls) {
    console.log(`Running Pa11y test on: ${url}`);
    const results = await pa11y(url, {
      browser,
      page,
      runner: 'puppeteer',
      timeout: 30000,
    });

    console.log(`Results for ${url}:`);
    console.log(results.issues.length > 0 ? results.issues : "No issues found.");
  }

  await browser.close();
})();
