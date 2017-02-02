package org.campagnelab.dl.genotype.performance;

import org.campagnelab.dl.genotype.helpers.GenotypeHelper;
import org.campagnelab.dl.genotype.predictions.GenotypePrediction;

import java.util.Arrays;

/**
 * Estimate genotype statistics.
 * Created by rct66 on 12/19/16.
 */
public class StatsAccumulator {


    int numCorrect;
    int numProcessed;
    int numTruePositive;
    int numTrueNegative;
    int numFalsePositive;
    int numFalseNegative;
    int numIndelsCorrect;
    int numSnpsCorrect;
    int numIndelsProcessed;
    int numSnpsProcessed;
    int numIndelsTruePositive;
    int numIndelsFalsePositive;
    int numIndelsFalseNegative;
    int numSnpsTruePositive;
    int numSnpsFalsePositive;
    int numSnpsFalseNegative;
    private int numVariants;
    private int concordantVariants;
    private int numVariantsExpected;
    private int numTrueOrPredictedVariants;

    public void initializeStats() {
        numCorrect = 0;
        numProcessed = 0;
        numTruePositive = 0;
        numTrueNegative = 0;
        numFalsePositive = 0;
        numFalseNegative = 0;
        numVariants = 0;
        concordantVariants = 0;
        numTrueOrPredictedVariants = 0;
        numIndelsCorrect = 0;
        numSnpsCorrect = 0;
        numIndelsProcessed = 0;
        numSnpsProcessed = 0;
        numIndelsTruePositive = 0;
        numIndelsFalsePositive = 0;
        numIndelsFalseNegative = 0;
        numSnpsTruePositive = 0;
        numSnpsFalsePositive = 0;
        numSnpsFalseNegative = 0;
    }

    public void observe(GenotypePrediction fullPred) {
        observe(fullPred, fullPred.isVariant(), fullPred.isVariant());
    }

    public void observe(GenotypePrediction fullPred, boolean isTrueVariant, boolean isPredictedVariant) {
        numProcessed++;
        if (isPredictedVariant || isTrueVariant) {
            numTrueOrPredictedVariants+=1;
            concordantVariants += fullPred.isCorrect()  ? 1 : 0;
        }

        if (fullPred.isCorrect()) {
            numCorrect++;
            if (isTrueVariant) {
                numTruePositive++;

            } else {
                numTrueNegative++;
            }
        } else {
            if (isTrueVariant) {
                numFalseNegative++;
            } else {
                numFalsePositive++;
            }
        }

        if (fullPred.isVariant()) {
            if (fullPred.isIndel()) {
                numIndelsProcessed++;
//                System.out.println("===============");
//                System.out.println("Curr indel #: " + numProcessed);
//                System.out.println("Curr indel predicted: " + fullPred.predictedGenotype);
//                System.out.println("Curr indel true: " + fullPred.trueGenotype);
                if (fullPred.isCorrect()) {
//                    System.out.println("Curr indel correct #: " + numProcessed);
                    numIndelsCorrect++;
                }
//                System.out.println("===============");
            } else {
                numSnpsProcessed++;
                if (fullPred.isCorrect()) {
                    numSnpsCorrect++;
                }
            }
        }

        if (fullPred.isPredictedIndel() && fullPred.isIndel()) {
            if (GenotypeHelper.matchingGenotypes(fullPred.trueGenotype, fullPred.predictedGenotype)) {
                numIndelsTruePositive++;
            } else {
                numIndelsFalseNegative++;
            }
        } else if (fullPred.isPredictedIndel() && !fullPred.isIndel()) {
            numIndelsFalsePositive++;
        } else if (!fullPred.isPredictedIndel() && fullPred.isIndel()) {
            numIndelsFalseNegative++;
        }

        if (fullPred.isPredictedSnp() && fullPred.isSnp()) {
            if (GenotypeHelper.matchingGenotypes(fullPred.trueGenotype, fullPred.predictedGenotype)) {
                numSnpsTruePositive++;
            } else {
                numSnpsFalseNegative++;
            }
        } else if (fullPred.isPredictedSnp() && !fullPred.isSnp()) {
            numSnpsFalsePositive++;
        } else if (!fullPred.isPredictedSnp() && fullPred.isSnp()) {
            numSnpsFalseNegative++;
        }



        numVariants += isTrueVariant ? 1 : 0;
        assert numVariants == numTruePositive + numFalseNegative;
    }

    public double[] createOutputStatistics() {
        double accuracy = numCorrect / (double) numProcessed;
        double indelAccuracy = numIndelsCorrect / (double) numIndelsProcessed;
        double snpAccuracy = numSnpsCorrect / (double) numSnpsProcessed;
        double genotypeConcordance = concordantVariants / (double) numTrueOrPredictedVariants;
        final int variantsExpected = Math.max(numTruePositive + numFalseNegative, this.numVariantsExpected);
        double recall = numTruePositive / ((double) variantsExpected);
        double precision = numTruePositive / ((double) (numTruePositive + numFalsePositive));
        // important fix. Remi, see https://en.wikipedia.org/wiki/F1_score
        double F1 = 2 * precision * recall / (precision + recall);
        double indelRecall = numIndelsTruePositive / ((double) numIndelsTruePositive + numIndelsFalseNegative);
        double indelPrecision = numIndelsTruePositive / ((double) numIndelsTruePositive + numIndelsFalsePositive);
        double indelF1 = 2 * indelPrecision * indelRecall / (indelPrecision + indelRecall);
        double snpRecall = numSnpsTruePositive / ((double) numSnpsTruePositive + numSnpsFalseNegative);
        double snpPrecision = numSnpsTruePositive / ((double) numSnpsTruePositive + numSnpsFalsePositive);
        double snpF1 = 2 * snpPrecision * snpRecall / (snpPrecision + snpRecall);
        return new double[]{accuracy, recall, precision, F1, numVariants, genotypeConcordance, indelAccuracy,
                indelRecall, indelPrecision, indelF1, snpAccuracy, snpRecall, snpPrecision, snpF1};
    }

    public double[] createOutputStatistics(String... metrics) {
        double[] estimates = createOutputStatistics();
        double[] values = new double[metrics.length];
        int i = 0;
        for (String metricName : metrics) {
            int j = -1;
            switch (metricName) {
                case "Accuracy":
                    j = 0;
                    break;
                case "Recall":
                    j = 1;
                    break;
                case "Precision":
                    j = 2;
                    break;
                case "F1":
                    j = 3;
                    break;
                case "NumVariants":
                    j = 4;
                    break;
                case "Concordance":
                    j = 5;
                    break;
                case "Accuracy_Indels":
                    j = 6;
                    break;
                case "Recall_Indels":
                    j = 7;
                    break;
                case "Precision_Indels":
                    j = 8;
                    break;
                case "F1_Indels":
                    j = 9;
                    break;
                case "Accuracy_SNPs":
                    j = 10;
                    break;
                case "Recall_SNPs":
                    j = 11;
                    break;
                case "Precision_SNPs":
                    j = 12;
                    break;
                case "F1_SNPs":
                    j = 13;
                    break;
                default:
                    throw new RuntimeException("performance metric not recognized: " + metricName);
            }
            values[i++] = estimates[j];
        }
        return values;
    }

    public String[] createOutputHeader() {
        return new String[]{"Accuracy", "Recall", "Precision", "F1", "NumVariants", "Concordance",
                "Accuracy_Indels", "Recall_Indels", "Precision_Indels", "F1_Indels",
                "Accuracy_SNPs", "Recall_SNPs", "Precision_SNPs", "F1_SNPs",
        };
    }

    public static final int F1_INDEX = 3;

    public void reportStatistics(String prefix) {
        double[] statsArray = createOutputStatistics();
        System.out.printf("Number of variants expected=%d%n", numVariantsExpected);
        System.out.println("Statistics estimated for " + prefix);
        System.out.println("Accuracy =" + statsArray[0]);
        System.out.println("Recall =" + statsArray[1]);
        System.out.println("Precision =" + statsArray[2]);
        System.out.println("F1 =" + statsArray[3]);
        System.out.println("numVariants =" + statsArray[4]);
        System.out.println("genotype concordance =" + statsArray[5]);
        System.out.println("Indel Accuracy =" + statsArray[6]);
        System.out.println("Indel Recall =" + statsArray[7]);
        System.out.println("Indel Precision =" + statsArray[8]);
        System.out.println("Indel F1 =" + statsArray[9]);
        System.out.println("SNP Accuracy =" + statsArray[10]);
        System.out.println("SNP Recall =" + statsArray[11]);
        System.out.println("SNP precision =" + statsArray[12]);
        System.out.println("SNP F1=" + statsArray[13]);
    }

    public void setNumVariantsExpected(int numVariantsExpected) {
        this.numVariantsExpected = numVariantsExpected;
    }
}
