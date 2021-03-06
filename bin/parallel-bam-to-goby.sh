#!/usr/bin/env bash
. `dirname "${BASH_SOURCE[0]}"`/setup.sh

assertGobyInstalled
assertParallelInstalled

if [ -e configure.sh ]; then
 echo "Loading configure.sh"
 source configure.sh
fi

ALIGNMENTS="$*"
if [ "$#" -eq 1 ]; then
   case ${ALIGNMENTS} in *.bam) OUTPUT_BASENAME=`basename ${ALIGNMENTS} .bam`;; esac
   case ${ALIGNMENTS} in *.sam) OUTPUT_BASENAME=`basename ${ALIGNMENTS} .sam`;; esac
   case ${ALIGNMENTS} in *.cram) OUTPUT_BASENAME=`basename ${ALIGNMENTS} .cram`;; esac
else
    OUTPUT_BASENAME="out-concat"
fi
echo "Will write Goby alignment to ${OUTPUT_BASENAME}"

if [ -z "${SBI_GENOME+set}" ]; then
    SBI_GENOME="/data/genomes/Homo_sapiens.ucsc.hg19"
    echo "SBI_GENOME set to ${SBI_GENOME}. Change the variable to influence the genome used (must be indexed with goby build-sequence-cache)."
fi
if [ -z "${FASTA_GENOME+set}" ]; then
    FASTA_GENOME="/data/genomes/Homo_sapiens.ucsc.hg19.fa"
    echo "FASTA_GENOME set to ${FASTA_GENOME}. Change the variable to influence the fasta used. Must come with a fasta index using same basename."
fi
if [ -z  "${SBI_NUM_THREADS+set}" ]; then
    SBI_NUM_THREADS="2"
    echo "SBI_NUM_THREADS set to ${SBI_NUM_THREADS}. Change the variable to influence the number of parallel jobs."
fi
echo "variables: ${SBI_GENOME} ${SBI_NUM_THREADS}"

goby 8g suggest-position-slices ${ALIGNMENTS} --number-of-slices 60 -o slices.tsv --restrict-per-chromosome
rm -rf calmd-and-convert-commands.txt
nLine=0
tail -n +2 slices.tsv | while read -r line
    do
       sRef=`echo $line | cut -f1 -d ' '`
       sPos=`echo $line | cut -f2 -d ' '`
       ePos=`echo $line | cut -f5 -d ' '`
       echo "samtools view -u ${ALIGNMENTS} ${sRef}:${sPos}-${ePos} > slice_${nLine}.bam ;\
         samtools calmd -E -u slice_${nLine}.bam ${FASTA_GENOME} > md_slice_${nLine}.bam ;\
         samtools index md_slice_${nLine}.bam &&\
         rm slice_${nLine}.bam ;\
         goby 8g concatenate-alignments --genome  ${SBI_GENOME}  md_slice_${nLine}.bam -o goby_slice_${nLine} &&\
         rm md_slice_${nLine}.bam &&\
         rm md_slice_${nLine}.bam.bai \
       " >> calmd-and-convert-commands.txt
       nLine=$((nLine+1))
done

parallel --bar -j${SBI_NUM_THREADS} --eta :::: calmd-and-convert-commands.txt

goby ${memory_requirement} concatenate-alignments goby_slice_*.entries -o ${OUTPUT_BASENAME} &&

rm goby_slice_*