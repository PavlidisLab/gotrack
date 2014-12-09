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

import java.io.Serializable;
import java.util.Date;

/**
 * This class represents the User model. This model class can be used thoroughout all layers, the data layer, the
 * controller layer and the view layer.
 */
public class User implements Serializable {

    // Constants ----------------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    // Properties ---------------------------------------------------------------------------------

    private Long id;
    private String email;
    private String password;
    private String firstname;
    private String lastname;
    private Date birthdate;

    // Getters/setters ----------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail( String email ) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword( String password ) {
        this.password = password;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname( String firstname ) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname( String lastname ) {
        this.lastname = lastname;
    }

    public Date getBirthdate() {
        return birthdate;
    }

    public void setBirthdate( Date birthdate ) {
        this.birthdate = birthdate;
    }

    // Object overrides ---------------------------------------------------------------------------

    /**
     * The user ID is unique for each User. So this should compare User by ID only.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object other ) {
        return ( other instanceof User ) && ( id != null ) ? id.equals( ( ( User ) other ).id ) : ( other == this );
    }

    /**
     * The user ID is unique for each User. So User with same ID should return same hashcode.
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return ( id != null ) ? ( this.getClass().hashCode() + id.hashCode() ) : super.hashCode();
    }

    /**
     * Returns the String representation of this User. Not required, it just pleases reading logs.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format( "User[id=%d,email=%s,firstname=%s,lastname=%s,birthdate=%s]", id, email, firstname,
                lastname, birthdate );
    }

}
