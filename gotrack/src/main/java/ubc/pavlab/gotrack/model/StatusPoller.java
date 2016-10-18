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

package ubc.pavlab.gotrack.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to acquire live updates from running methods which can then be displayed using EL in the front-end. Assume not
 * thread-safe.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class StatusPoller {

    private int progress = 0;
    private List<String> statuses = new ArrayList<>();
    private final String completionAppend;
    private boolean previousHasCompleted = false;
    private String currentStatus = "";

    public StatusPoller( String completionAppend ) {
        this.completionAppend = completionAppend;
    }

    public StatusPoller() {
        this.completionAppend = "";
    }

    public synchronized int getProgress() {
        return progress;
    }

    public synchronized void setProgress( int progress ) {
        this.progress = progress;
    }

    public synchronized List<String> getStatuses() {
        return statuses;
    }

    public synchronized String getCurrentStatus() {
        return currentStatus;
    }

    public synchronized void newStatus( String status, int progress ) {
        if ( progress > 100 ) throw new RuntimeException( "Progress cannot be > 100%" );
        if ( progress < this.progress ) throw new RuntimeException( "Progress cannot go backwards" );
        previousHasCompleted = false;
        this.progress = progress;
        statuses.add( status );
        currentStatus = status;
    }

    public synchronized void completeStatus() {
        if ( previousHasCompleted ) throw new RuntimeException( "Status already complete" );
        previousHasCompleted = true;
        String status = statuses.get( statuses.size() - 1 );
        statuses.set( statuses.size() - 1, status + completionAppend );
    }

}
