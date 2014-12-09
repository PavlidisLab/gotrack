/*
 * The gotrack project
 * 
 * Copyright (c) 2014 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubc.pavlab.gotrack;

import java.text.SimpleDateFormat;
import java.util.List;

import ubc.pavlab.gotrack.dao.DAOFactory;
import ubc.pavlab.gotrack.dao.UserDAO;

/**
 * Test harness for the com.example.dao package. This require the following preconditions:
 * <ol>
 * <li>A MySQL server running at localhost:3306 with a database named 'gotrack'.
 * <li>A 'user' table in the 'gotrack' database which is created as follows:
 * 
 * <pre>
 * CREATE TABLE javabase.user (
 *     id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
 *     email VARCHAR(60) NOT NULL,
 *     password VARCHAR(32) NOT NULL,
 *     firstname VARCHAR(40) NULL,
 *     lastname VARCHAR(40) NULL,
 *     birthdate DATE NULL,
 * 
 *     PRIMARY KEY (id),
 *     UNIQUE (email)
 * )
 * </pre>
 * <li>A MySQL user with the name 'gotrack' and password 'gotrack321#' which has sufficient rights on the gotrack.user
 * table.
 * <li>A MySQL JDBC Driver JAR file in the classpath.
 * <li>A properties file 'database.properties' in the classpath with the following entries:
 * 
 * <pre>
 * gotrack.driver = com.mysql.jdbc.Driver
 * jgotrack.url = jdbc:mysql://localhost:3306/gotrack
 * gotrack.username = gotrack
 * gotrack.password = gotrack321#
 * </pre>
 * 
 * </ol>
 */
public class DAOTest {

    public static void main( String[] args ) throws Exception {
        // Obtain DAOFactory.
        DAOFactory gotrack = DAOFactory.getInstance( "gotrack" );
        System.out.println( "DAOFactory successfully obtained: " + gotrack );

        // Obtain UserDAO.
        UserDAO userDAO = gotrack.getUserDAO();
        System.out.println( "UserDAO successfully obtained: " + userDAO );

        // Create user.
        User user = new User();
        user.setEmail( "foo@bar.com" );
        user.setPassword( "password" );
        userDAO.create( user );
        System.out.println( "User successfully created: " + user );

        // Create another user.
        User anotherUser = new User();
        anotherUser.setEmail( "bar@foo.com" );
        anotherUser.setPassword( "anotherPassword" );
        anotherUser.setFirstname( "Bar" );
        anotherUser.setLastname( "Foo" );
        anotherUser.setBirthdate( new SimpleDateFormat( "yyyy-MM-dd" ).parse( "1978-03-26" ) );
        userDAO.create( anotherUser );
        System.out.println( "Another user successfully created: " + anotherUser );

        // Update user.
        user.setFirstname( "Foo" );
        user.setLastname( "Bar" );
        userDAO.update( user );
        System.out.println( "User successfully updated: " + user );

        // Update user.
        user.setFirstname( "Foo" );
        user.setLastname( "Bar" );
        userDAO.update( user );
        System.out.println( "User successfully updated: " + user );

        // List all users.
        List<User> users = userDAO.list();
        System.out.println( "List of users successfully queried: " + users );
        System.out.println( "Thus, amount of users in database is: " + users.size() );

        // Delete user.
        userDAO.delete( user );
        System.out.println( "User successfully deleted: " + user );

        // Check if email exists.
        boolean exist = userDAO.existEmail( "foo@bar.com" );
        System.out.println( "This email should not exist anymore, so this should print false: " + exist );

        // Change password.
        anotherUser.setPassword( "newAnotherPassword" );
        userDAO.changePassword( anotherUser );
        System.out.println( "Another user's password successfully changed: " + anotherUser );

        // Get another user by email and password.
        User foundAnotherUser = userDAO.find( "bar@foo.com", "newAnotherPassword" );
        System.out.println( "Another user successfully queried with new password: " + foundAnotherUser );

        // Delete another user.
        userDAO.delete( foundAnotherUser );
        System.out.println( "Another user successfully deleted: " + foundAnotherUser );

        // List all users again.
        users = userDAO.list();
        System.out.println( "List of users successfully queried: " + users );
        System.out.println( "Thus, amount of users in database is: " + users.size() );
    }
}
