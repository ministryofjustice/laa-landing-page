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
    // Wait for email input and type username
    await page.waitForSelector('#email', { visible: true });
    await page.type('#email', username);
  
    // Click "Sign in" (email submit)
    const html = await page.content();
    console.log(html);
    await page.waitForSelector('input[type="submit"]', { visible: true });
    await page.click('input[type="submit"]');
    await page.waitForNavigation({ waitUntil: 'networkidle2' });
  
    // Wait for password input and type password
    await page.waitForSelector('input[type="password"]', { visible: true });
    await page.type('input[type="password"]', password);
  
    // Click "Sign in" (password submit)
    await page.waitForSelector('input[type="submit"]', { visible: true });
    const frames = page.frames();
    console.log('Frames on page:', frames.map(f => f.url()));
    await page.click('input[type="submit"]');
    await page.waitForNavigation({ waitUntil: 'networkidle2' });
  
    // Handle "Stay signed in?" prompt if it appears
    const staySignedInButton = await page.$('#idBtn_Back'); // or idBtn_Foreground
    if (staySignedInButton) {
      console.log('Handling "Stay signed in?" screen...');
      await staySignedInButton.click();
      await page.waitForNavigation({ waitUntil: 'networkidle2' });
    }
  
    console.log('Login successful');
  
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
