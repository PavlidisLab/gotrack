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

package ubc.pavlab.gotrack.analysis;

/**
 * Simple container for passing around the results of EnrichmentView's analysis
 * 
 * @author mjacobson
 * @version $Id$
 */
public final class CombinedAnalysis {
    private final EnrichmentAnalysis enrichmentAnalysis;
    private final StabilityAnalysis stabilityAnalysis;
    private final SimilarityAnalysis similarityAnalysis;
    private final boolean success;

    public CombinedAnalysis( EnrichmentAnalysis enrichmentAnalysis, StabilityAnalysis stabilityAnalysis,
            SimilarityAnalysis similarityAnalysis,
            boolean success ) {
        super();
        this.enrichmentAnalysis = enrichmentAnalysis;
        this.stabilityAnalysis = stabilityAnalysis;
        this.similarityAnalysis = similarityAnalysis;
        this.success = success;

    }

    public EnrichmentAnalysis getEnrichmentAnalysis() {
        return enrichmentAnalysis;
    }

    public StabilityAnalysis getStabilityAnalysis() {
        return stabilityAnalysis;
    }

    public SimilarityAnalysis getSimilarityAnalysis() {
        return similarityAnalysis;
    }

    public boolean isSuccess() {
        return success;
    }

}
