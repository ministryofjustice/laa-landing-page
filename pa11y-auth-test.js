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

  // Navigate to login page
  await page.goto(urls[0], { waitUntil: 'networkidle2' });

  // Simulate login - you MUST adjust selectors to match your login page
  try {
    // Step 1: Enter username/email
    await page.waitForSelector('input[type="email"]', { visible: true });
    await page.type('input[type="email"]', username);
    
    const buttons = await page.$$eval('button, input', els =>
        els.map(el => ({ text: el.innerText, id: el.id, type: el.type }))
      );
    console.log('Available buttons:', buttons);

    await page.screenshot({ path: 'before-failure.png' });
    await page.waitForSelector('input[type="submit"], button[type="submit"], #idSIButton9', { visible: true });
    await page.click('input[type="submit"], button[type="submit"], #idSIButton9');
    await page.waitForNavigation({ waitUntil: 'networkidle2' });
  
    // Step 2: Enter password
    await page.waitForSelector('input[type="password"]', { visible: true });
    await page.type('input[type="password"]', password);
    await page.waitForSelector('#idSIButton9', { visible: true });
    await page.click('#idSIButton9');
    await page.waitForNavigation({ waitUntil: 'networkidle2' });
  
    // Optional: Handle "Stay signed in?" prompt
    const staySignedInSelector = 'input[id="idBtn_Back"], input[id="idBtn_Foreground"]';
    const staySignedInExists = await page.$(staySignedInSelector);
    if (staySignedInExists) {
      await page.click(staySignedInSelector); // Choose Back or Foreground based on your flow
      await page.waitForNavigation({ waitUntil: 'networkidle2' });
    }
  
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
