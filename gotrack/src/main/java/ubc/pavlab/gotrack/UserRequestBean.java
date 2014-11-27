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

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@ManagedBean
@SessionScoped
public class UserRequestBean {

    private String message = "Hello World!";
    private String[] popularGenes = { "spades", "hearts", "diamonds", "clubs" };
    private String[] topMultifunc = { "One", "Two", "Three", "Four" };
    private String userInput = "";
    private String userSpecies = "";

    /**
     * 
     */
    public UserRequestBean() {
        // TODO Auto-generated constructor stub
    }

    public String getMessage() {
        return message;
    }

    public String[] getPopularGenes() {
        return popularGenes;
    }

    public String[] getTopMultifunc() {
        return topMultifunc;
    }

    public String getUserInput() {
        return userInput;
    }

    public String getUserSpecies() {
        return userSpecies;
    }
}
