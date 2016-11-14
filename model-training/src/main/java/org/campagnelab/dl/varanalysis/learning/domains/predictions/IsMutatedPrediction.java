package org.campagnelab.dl.varanalysis.learning.domains.predictions;

import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;

/**
 * Created by fac2003 on 11/12/16.
 */
public class IsMutatedPrediction extends BinaryClassPrediction {

    public <BaseInformation> void inspectRecord(BaseInformationRecords.BaseInformation currentRecord) {

        trueLabelYes = currentRecord.hasMutated() ? (currentRecord.getMutated() ? 1.0 : 0.0 ): null;

    }
}
