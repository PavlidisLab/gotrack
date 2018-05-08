/*
 * The gotrack project
 * 
 * Copyright (c) 2016 University of British Columbia
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

package ubc.pavlab.gotrack.model.rest.marshal;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ubc.pavlab.gotrack.analysis.MultipleTestCorrection;
import ubc.pavlab.gotrack.analysis.SimilarityMethod;
import ubc.pavlab.gotrack.model.Aspect;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Set;

/**
 * @author mjacobson
 */
@XmlRootElement
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EnrichmentHistoricalRequest {
    @XmlElement
    public int speciesId;
    @XmlElement
    public int month;
    @XmlElement
    public int year;
    @XmlElement
    public List<String> genes;
    @XmlElement
    public Set<Aspect> aspects = Sets.newHashSet( Aspect.BP );
    @XmlElement
    public double threshold = 0.05;
    @XmlElement
    public int min = 20;
    @XmlElement
    public int max = 200;
    @XmlElement
    public int topN = 5;
    @XmlElement
    public SimilarityMethod similarityMethod = SimilarityMethod.JACCARD;
    @XmlElement
    public MultipleTestCorrection multipleTestCorrection = MultipleTestCorrection.BH;
    @XmlElement
    public boolean includeSets = true;


}
