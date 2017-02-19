package org.campagnelab.dl.genotype.predictions;

import org.campagnelab.dl.framework.domains.prediction.PredictionInterpreter;
import org.campagnelab.dl.genotype.helpers.GenotypeHelper;
import org.campagnelab.dl.genotype.mappers.MetaDataLabelMapper;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * This interpreter extracts meta-data (isVariant, isIndel) from the meta-data label.
 * Created by fac2003 on 12/22/16.
 */
public class MetaDataInterpreter implements PredictionInterpreter<BaseInformationRecords.BaseInformation,
        MetadataPrediction> {

    @Override
    public MetadataPrediction interpret(INDArray trueLabels, INDArray output, int predictionIndex) {
        MetadataPrediction p = new MetadataPrediction();
        p.isIndel = trueLabels.getDouble(predictionIndex, MetaDataLabelMapper.IS_INDEL_FEATURE_INDEX) == 1;
        p.isVariant = trueLabels.getDouble(predictionIndex, MetaDataLabelMapper.IS_VARIANT_FEATURE_INDEX) == 1;
        p.referenceGobyIndex=(int)trueLabels.getDouble(predictionIndex,MetaDataLabelMapper.IS_MATCHING_REF_FEATURE_INDEX);
        return p;
    }

    @Override
    public MetadataPrediction interpret(BaseInformationRecords.BaseInformation record, INDArray output) {
        MetadataPrediction p = new MetadataPrediction();
        p.isVariant = record.getSamples(0).getIsVariant();
        final String trueGenotype = record.getTrueGenotype();
        p.isIndel =GenotypeHelper.isIndel(record.getReferenceBase(), trueGenotype) ;
        return p;
    }
}
