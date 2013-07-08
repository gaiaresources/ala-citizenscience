/**
 * 
 */
package au.com.gaiaresources.bdrs.controller.webservice;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import au.com.gaiaresources.bdrs.json.JSON;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.json.JSONSerializer;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.model.group.Group;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.user.User;

import javax.servlet.http.HttpServletResponse;

/**
 * @author timo
 * 
 */
public class UserServiceTest extends AbstractControllerTest {
	
	Logger log = Logger.getLogger(UserServiceTest.class);

    private static final String BYTE_ENCODING = "UTF-8";
	
	@Before
	public void setup() throws Exception{
		 //create a user
		 PasswordEncoder passwordEncoder = new Md5PasswordEncoder();
	     String emailAddr = "user@mailinator.com";
	     String encodedPassword = passwordEncoder.encodePassword("password", null);
	     userDAO.createUser("user", "fn", "ln", emailAddr, encodedPassword, "usersIdent", new String[] { "ROLE_USER" });
	}
	
	
	@Test
	public void testPing() throws Exception {
		request.setMethod("POST");
		request.setRequestURI("/webservice/user/ping.htm");
		handle(request, response);
		Assert.assertEquals("Content type should be text/javascript",
				"text/javascript", response.getContentType());
		Assert.assertEquals("Response from ping should be null({0:1});",
				"null({0:1});", response.getContentAsString());
	}
	
	/**
	 * Tests when user is trying to validate with an existing username and password.
	 * @throws Exception
	 */
	@Test
	public void testValidateUser() throws Exception{
		
		request.setMethod("POST");
		request.setRequestURI("/webservice/user/validate.htm");
		request.addParameter("username", "user");
		request.addParameter("password", "password");
		
		handle(request, response);
		JSONObject validUser = new JSONObject();
		JSONObject validationResponse = JSONObject.fromStringToJSONObject(response.getContentAsString());
		
		if(validationResponse.containsKey("user")){
			validUser = validationResponse.getJSONObject("user");
		}
		Assert.assertEquals("Content type should be application/json", "application/json", response.getContentType());
		Assert.assertEquals("The name of the user should be 'user' and it is not.", "user", validUser.getString("name"));
		Assert.assertEquals("The lastName of the user should be 'ln' and it is not.", "ln", validUser.getString("lastName"));
	}
	
	/**
	 * Tests when user is trying to validate with a username but no password.
	 * @throws Exception
	 */
	@Test
	public void testValidateUser1() throws Exception{
		request.setMethod("POST");
		request.setRequestURI("/webservice/user/validate.htm");
		request.addParameter("username", "user");
		request.addParameter("password", "");
		
		handle(request, response);
		
		// validation error, shouldn't get anything back
		Assert.assertEquals("", response.getContentAsString());	}
	
	/**
	 * Tests when user is trying to validate with a password but no username.
	 * @throws Exception
	 */
	@Test
	public void testValidateUser2() throws Exception{
		request.setMethod("POST");
		request.setRequestURI("/webservice/user/validate.htm");
		request.addParameter("username", "");
		request.addParameter("password", "password");
		
		handle(request, response);
		// validation error, shouldn't get anything back
		Assert.assertEquals("", response.getContentAsString());
	}
	
	//private static final String expectedJson = "{\"total\":\"2\",\"page\":\"1\",\"records\":\"5\",\"rows\":[{\"cellValues\":[\"one\",\"two\",\"three\"],\"id\":1},{\"cellValues\":[\"four\",\"five\",\"six\"],\"id\":2},{\"cellValues\":[\"seven\",\"eight\",\"nine\"],\"id\":3}]}";
	@Test
	public void testJqGridDataBuilder() {
	    JqGridDataBuilder builder = new JqGridDataBuilder(3, 5, 1);
	    builder.addRow(new JqGridDataRow(1).addValue("field1", "1").addValue("field2", "2").addValue("field3", "3"));
	    builder.addRow(new JqGridDataRow(2).addValue("field1", "4").addValue("field2", "4").addValue("field3", "4"));
	    builder.addRow(new JqGridDataRow(3).addValue("field1", "5").addValue("field2", "5").addValue("field3", "5"));
	    //Assert.assertEquals(expectedJson, builder.toJson());
	    String json = builder.toJson();
	    JSONObject obj = JSONObject.fromStringToJSONObject(json);
	    Assert.assertEquals("5", obj.get("records"));
	    JSONArray rows = (JSONArray)obj.get("rows");
	    Assert.assertEquals(3, rows.size());
	    Assert.assertEquals("1", ((JSONObject)rows.get(0)).get("id"));
	    Assert.assertEquals("1", ((JSONObject)rows.get(0)).get("field1"));
	    Assert.assertEquals("2", ((JSONObject)rows.get(0)).get("field2"));
	    Assert.assertEquals("3", ((JSONObject)rows.get(0)).get("field3"));
	    
	    Assert.assertEquals("2", ((JSONObject)rows.get(1)).get("id"));
	    Assert.assertEquals("4", ((JSONObject)rows.get(1)).get("field1"));
	    Assert.assertEquals("4", ((JSONObject)rows.get(1)).get("field2"));
	    Assert.assertEquals("4", ((JSONObject)rows.get(1)).get("field3"));
	}

    @Test
    public void testRegister() throws Exception {

        String username = "jimmyboy";
        String firstname = "jimmy";
        String lastname = "recard";
        String email = "jimmy.recard@testemail.com";
        String password = "01mysupersecurepw";

        JSONObject details = new JSONObject();
        details.put("name", username);
        details.put("first_name", firstname);
        details.put("last_name", lastname);
        details.put("email_address", email);
        details.put("password", password);
        details.put("active", "1"); // make user active immediately.

        String query = details.toString();

        MessageDigest m = MessageDigest.getInstance("MD5");
        String key = (query + UserService.MAGIC_KEY);
        m.update(key.getBytes(BYTE_ENCODING),0,key.length());
        String mySig = new BigInteger(1,m.digest()).toString(16);
        while (mySig.length() < 32) {
            mySig = "0" + mySig;
        }

        request.setMethod("POST");
        request.setRequestURI("/webservice/user/registerUser.htm");
        request.addParameter("details", query);
        request.addParameter("signature", mySig);

        handle(request, response);

        Assert.assertEquals("wrong response code", HttpServletResponse.SC_OK, response.getStatus());
        Assert.assertFalse("response should be non empty", response.getContentAsString().isEmpty());

        JSONObject jsonResponse = JSONObject.fromStringToJSONObject(response.getContentAsString());

        Assert.assertFalse("we should get a return ident", jsonResponse.getString("ident").isEmpty());


        // try to make another user with the same name
        request = this.createStandardRequest();
        response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.setRequestURI("/webservice/user/registerUser.htm");
        request.addParameter("details", query);
        request.addParameter("signature", mySig);

        handle(request, response);

        Assert.assertEquals("wrong response code", HttpServletResponse.SC_OK, response.getStatus());
        Assert.assertFalse("response should be non empty", response.getContentAsString().isEmpty());
        JSONObject existingUserResponse = JSONObject.fromStringToJSONObject(response.getContentAsString());
        Assert.assertTrue("user exists flag should be true", existingUserResponse.getBoolean("userExists"));


        // now see if we can log in...
        request = this.createStandardRequest();

        request.setMethod("POST");
        request.setRequestURI("/webservice/user/validate.htm");
        request.addParameter("username", username);
        request.addParameter("password", password);

        response = new MockHttpServletResponse();

        handle(request, response);

        JSONObject validationResponse = JSONObject.fromStringToJSONObject(response.getContentAsString());

        Assert.assertTrue("user key should exist", validationResponse.has("user"));
        JSONObject validUser = validationResponse.getJSONObject("user");
        Assert.assertEquals("Content type should be application/json", "application/json", response.getContentType());
        Assert.assertEquals("wrong username", username, validUser.getString("name"));
        Assert.assertEquals("wrong lastname", lastname, validUser.getString("lastName"));

        User u = userDAO.getUser(username);
        Assert.assertTrue("user should be active", u.isActive());
    }

    @Test
    public void testRegisterNonActive() throws Exception {
        String username = "jimmyboy";
        String firstname = "jimmy";
        String lastname = "recard";
        String email = "jimmy.recard@testemail.com";
        String password = "01mysupersecurepw";

        JSONObject details = new JSONObject();
        details.put("name", username);
        details.put("first_name", firstname);
        details.put("last_name", lastname);
        details.put("email_address", email);
        details.put("password", password);
        //details.put("active", "0"); user should be non active

        String query = details.toString();

        MessageDigest m = MessageDigest.getInstance("MD5");
        String key = (query + UserService.MAGIC_KEY);
        m.update(key.getBytes(BYTE_ENCODING),0,key.length());
        String mySig = new BigInteger(1,m.digest()).toString(16);
        while (mySig.length() < 32) {
            mySig = "0" + mySig;
        }

        request.setMethod("POST");
        request.setRequestURI("/webservice/user/registerUser.htm");
        request.addParameter("details", query);
        request.addParameter("signature", mySig);

        handle(request, response);

        Assert.assertEquals("wrong response code", HttpServletResponse.SC_OK, response.getStatus());
        Assert.assertFalse("response should be non empty", response.getContentAsString().isEmpty());

        JSONObject jsonResponse = JSONObject.fromStringToJSONObject(response.getContentAsString());

        Assert.assertFalse("we should get a return ident", jsonResponse.getString("ident").isEmpty());

        // now see if we can log in...

        // validate actually lets us validate an inactive user... so we check the active flag
        request = this.createStandardRequest();

        request.setMethod("POST");
        request.setRequestURI("/webservice/user/validate.htm");
        request.addParameter("username", username);
        request.addParameter("password", password);

        response = new MockHttpServletResponse();

        handle(request, response);

        JSONObject validationResponse = JSONObject.fromStringToJSONObject(response.getContentAsString());

        Assert.assertTrue("user key should exist", validationResponse.has("user"));
        JSONObject validUser = validationResponse.getJSONObject("user");
        Assert.assertEquals("Content type should be application/json", "application/json", response.getContentType());
        Assert.assertEquals("wrong username", username, validUser.getString("name"));
        Assert.assertEquals("wrong lastname", lastname, validUser.getString("lastName"));

        User u = userDAO.getUser(username);
        Assert.assertFalse("user should be inactive", u.isActive());
    }

    @Test
    public void testRegisterBadSig() throws Exception {
        String username = "jimmyboy";
        String firstname = "jimmy";
        String lastname = "recard";
        String email = "jimmy.recard@testemail.com";
        String password = "01mysupersecurepw";

        JSONObject details = new JSONObject();
        details.put("name", username);
        details.put("first_name", firstname);
        details.put("last_name", lastname);
        details.put("email_address", email);
        details.put("password", password);
        details.put("active", "1"); // make user active immediately.

        String query = details.toString();

        String mySig = "bogussignature";

        request.setMethod("POST");
        request.setRequestURI("/webservice/user/registerUser.htm");
        request.addParameter("details", query);
        request.addParameter("signature", mySig);

        handle(request, response);

        Assert.assertEquals("wrong response code", HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }
}
