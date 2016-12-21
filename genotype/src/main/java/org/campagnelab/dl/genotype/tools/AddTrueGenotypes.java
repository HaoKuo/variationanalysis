package org.campagnelab.dl.genotype.tools;


import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.util.XorShift1024StarRandom;
import org.campagnelab.dl.framework.tools.arguments.AbstractTool;
import org.campagnelab.dl.somatic.storage.RecordReader;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;
import org.campagnelab.goby.baseinfo.SequenceBaseInformationWriter;
import org.campagnelab.goby.reads.RandomAccessSequenceCache;
import org.campagnelab.goby.reads.RandomAccessSequenceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

/**
 * The addcalls object uses a map to create a new protobuf file with genotype calls.
 * <p>
 * Created by rct66 on 5/18/16.
 *
 * @author rct66
 */
public class AddTrueGenotypes extends AbstractTool<AddTrueGenotypesArguments> {


    int numVariantsAdded = 0;
    RandomAccessSequenceCache genome = new RandomAccessSequenceCache();

    static private Logger LOG = LoggerFactory.getLogger(AddTrueGenotypes.class);

    public static void main(String[] args) {

        AddTrueGenotypes tool = new AddTrueGenotypes();
        tool.parseArguments(args, "AddTrueGenotypes", tool.createArguments());
        tool.execute();

    }


    @Override
    //only supports genotypes encoded with a bar (|) delimiter
    public void execute() {


        //get reference genome
        String genomePath = args().genomeFilename;
        try {
            System.err.println("Loading genome cache " + genomePath);
            genome = new RandomAccessSequenceCache();
            genome.load(genomePath, "min", "max");
            System.err.println("Done loading genome. ");
        } catch (ClassNotFoundException e) {
            System.err.println("Could not load genome cache");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Could not load genome cache");
            e.printStackTrace();
            System.exit(1);
        }

        int sampleIndex = args().sampleIndex;
        Random random = new XorShift1024StarRandom();
        try {
            Object2ObjectMap<String,Int2ObjectMap<String>> chMap = (Object2ObjectMap<String,Int2ObjectMap<String>>)BinIO.loadObject(args().genotypeMap);
            RecordReader source = new RecordReader(args().inputFile);
            SequenceBaseInformationWriter dest = new SequenceBaseInformationWriter(args().outputFilename);

            ProgressLogger recordLogger = new ProgressLogger(LOG);
            recordLogger.expectedUpdates = source.numRecords();
            System.out.println(source.numRecords() + " records to label");
            int recordsLabeled = 0;
            recordLogger.start();
            for (BaseInformationRecords.BaseInformation rec : source) {
                boolean skip = false;






                BaseInformationRecords.BaseInformation.Builder buildRec = rec.toBuilder();
                int position = buildRec.getPosition();
                String chrom = buildRec.getReferenceId();
                String[] genotypes = new String[3];
                String trueGenotype;
                boolean isVariant = false;
                if (chMap.get(chrom) != null && chMap.get(chrom).get(position) != null) {
                    // The map contains Goby positions (zero-based).
                    trueGenotype = chMap.get(chrom).get(position);
                    trueGenotype = trueGenotype.toUpperCase();
                    genotypes = trueGenotype.split("|");
                    isVariant = true;
                    numVariantsAdded++;
                } else {
                    if (random.nextFloat() > args().referenceSamplingRate) {
                        skip = true;
                    }
                    String referenceBase = Character.toString(genome.get(buildRec.getReferenceIndex(),buildRec.getPosition()));
                    trueGenotype = referenceBase+"|"+referenceBase;
                    referenceBase = referenceBase.toUpperCase();
                    genotypes[0] = referenceBase;
                    genotypes[2] = referenceBase;
                }
                if (!skip) {
                    // write the record.
                    buildRec.setTrueGenotype(trueGenotype.replace('|', '/').toUpperCase());
                    BaseInformationRecords.SampleInfo.Builder buildSample = buildRec.getSamples(sampleIndex).toBuilder();
                    for (int i = 0; i < buildSample.getCountsCount(); i++) {
                        BaseInformationRecords.CountInfo.Builder count = buildSample.getCounts(i).toBuilder();
                        boolean isCalled = (count.getToSequence().equals(genotypes[0]) || count.getToSequence().equals(genotypes[2]));
                        count.setIsCalled(isCalled);
                        buildSample.setCounts(i, count);
                    }
                    buildSample.setIsVariant(isVariant);
                    buildRec.setSamples(sampleIndex, buildSample.build());
                    dest.appendEntry(buildRec.build());
                    recordsLabeled++;
                }
                recordLogger.lightUpdate();
            }
            recordLogger.done();
            dest.close();
            System.out.println(numVariantsAdded + " number of variants in the sbi file.");
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public AddTrueGenotypesArguments createArguments() {
        return new AddTrueGenotypesArguments();
    }



}
