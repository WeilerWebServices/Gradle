<h1>Testing</h1>

<p>Geb provides integration modules for popular testing frameworks such as <a href="http://spockframework.org">Spock</a>, <a href="http://junit.org">JUnit</a>, <a href="http://testng.org">TestNG</a>, <a href="http://www.easyb.org/">EasyB</a> and <a href="http://cukes.info/">Cucumber</a> (via <a href="https://github.com/cucumber/cuke4duke/wiki">Cuke4Duke</a>)</p>

<p>While Geb works great with all of these frameworks, it really shines with <a href="http://spockframework.org">Spock</a>. Spock is an innovative testing framework that is a great match for using with Geb. Using Spock + Geb gives you very clear, concise and easy to understand test specifications with very little effort.</p>

<pre class="brush: groovy">import geb.Page
import geb.spock.GebSpec

class LoginSpec extends GebSpec {
    def "login to admin section"() {
        given:
        to LoginPage
        
        when:
        loginForm.with {
            username = "admin"
            password = "password"
        }
        
        and:
        loginButton.click()
        
        then:
        at AdminPage
    }
}
</pre>

<p>See the <a href="manual/current/testing.html">manual section on testing</a> for more information on using Geb with Spock as well as other testing frameworks.</p>