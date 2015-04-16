/*
 * The gotrack project
 * 
 * Copyright (c) 2015 University of British Columbia
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

package ubc.pavlab.gotrack.model.go;

import gnu.trove.set.hash.THashSet;

import java.util.Set;

import ubc.pavlab.gotrack.model.Aspect;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class Term {

    private int id;
    private String name;
    private Aspect aspect;
    private boolean obsolete;
    // private Set<Term> parents = new HashSet<>();
    private THashSet<Link> parents = new THashSet<>();

    public Term() {

    }

    public Term( int id ) {
        super();
        this.id = id;
    }

    public Term( int id, String name, Aspect aspect ) {
        super();
        this.id = id;
        this.name = name;
        this.aspect = aspect;
    }

    public Term( int id, String name, Aspect aspect, boolean obsolete ) {
        super();
        this.id = id;
        this.name = name;
        this.aspect = aspect;
        this.obsolete = obsolete;
    }

    public Term( int id, String name, Aspect aspect, boolean obsolete, THashSet<Link> parents ) {
        super();
        this.id = id;
        this.name = name;
        this.aspect = aspect;
        this.obsolete = obsolete;
        this.parents = parents;
    }

    public int getId() {
        return id;
    }

    public void setId( int id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public Aspect getAspect() {
        return aspect;
    }

    public void setAspect( Aspect aspect ) {
        this.aspect = aspect;
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public void setObsolete( boolean obsolete ) {
        this.obsolete = obsolete;
    }

    public Set<Link> getParents() {
        return parents;
    }

    public void setParents( THashSet<Link> parents ) {
        this.parents = parents;
    }

    public boolean addParent( Link parent ) {
        return parents.add( parent );
    }

    public boolean containsParent( Term parent ) {
        return parents.contains( parent );
    }

    @Override
    public String toString() {
        return "Term [id=" + id + ", name=" + name + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        Term other = ( Term ) obj;
        if ( id != other.id ) return false;
        return true;
    }

}
