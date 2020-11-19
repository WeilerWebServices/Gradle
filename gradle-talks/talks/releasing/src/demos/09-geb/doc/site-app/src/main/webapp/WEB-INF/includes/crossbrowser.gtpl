<h1>Cross Browser Automation</h1>

<p>Geb leverages the <a href="http://code.google.com/p/selenium/">WebDriver</a> library for browser automation. This means that Geb works with any browser that WebDriver works with, and the list of browsers that WebDriver works with is growing all the time.</p>

<p>The core supported browsers are:</p>

<ul>
    <li><a href="http://code.google.com/p/selenium/wiki/FirefoxDriver">FireFox</a></li>
    <li><a href="http://code.google.com/p/selenium/wiki/InternetExplorerDriver">Internet Explorer</a></li>
    <li><a href="http://code.google.com/p/selenium/wiki/ChromeDriver">Google Chrome</a></li>
    <li><a href="http://www.opera.com/developer/tools/operadriver/" title="OperaDriver | Opera Developer Tools">Opera</a></li>
</ul>

<p>There is also experimental support for:</p>

<ul>
    <li><a href="http://code.google.com/p/selenium/wiki/AndroidDriver">Chrome on Android</a></li>
    <li><a href="http://code.google.com/p/selenium/wiki/IPhoneDriver">Safari on iPhone &amp; iPad</a></li>
</ul>

<h1>Remote Browsers</h1>

<p>WebDriver also supports <a href="http://code.google.com/p/selenium/wiki/RemoteWebDriver"><em>remote drivers</em></a>. This allows you to automate a browser running on another machine! This means you can easily run your test suite against an IE browser from the comfort of your Mac or Linux machine (and vice versa).</p>

<h1>Headless Browsers</h1>

<p>You can also use the headless, in process, Java browser emulator <a href="http://code.google.com/p/selenium/wiki/HtmlUnitDriver">HTMLUnit with WebDriver</a>.</p>

<p>See the <a href="manual/current/configuration.html#driver_implementation">driver configuration section of the manual</a> for information about setting up Geb to run with different drivers.</p>