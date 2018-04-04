package ubc.pavlab.gotrack.analysis;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ubc.pavlab.gotrack.model.Aspect;

import java.util.List;

/**
 * Created by mjacobson on 14/03/18.
 */
@Getter
@Setter
@NoArgsConstructor
public class EnrichmentAnalysisOptions {

    private int minAnnotatedPopulation = 5; // minimum geneset size a specific term must have to be included in results
    private int maxAnnotatedPopulation = 200; // maximum geneset size a specific term must have to be included in results
    private MultipleTestCorrection multipleTestCorrection = MultipleTestCorrection.BH; // method of multiple tests correction
    private double threshold = 0.05; // tEither p-value cutoff if using Bonferroni or FDR level if using BH step-up
    private List<Aspect> aspects = Lists.newArrayList( Aspect.BP ); // Aspect restriction
}
