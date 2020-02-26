/*
 * This class contains code related to MapReduce job operations on the Amazon Customer Review Dataset.
 * The class is responsible for starting the Map and Reduce workers.
 * Responsible: Cetin Tekin
 */

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.io.IOException;
import java.util.HashMap;
import org.apache.hadoop.io.SequenceFile.Reader.Option;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;


public class MROperations {

    private Class<? extends Reducer> statisticalReducer;         /* Statistical reducer class that is used in Reduce and Combine workers */
    FileSystem fs;                                               /* Used for HDFS i/o */
    Configuration conf;                                          /* Used for Hadoop worker configuration */


    /* Sets the statistical reducer class according to given parameter */
    public void setStatisticalReducer(Class<? extends Reducer> statisticalReducer) {
        this.statisticalReducer = statisticalReducer;
    }

    /* Creates configuration for MapReduce job */
    private void createConfig() {
        conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://172.20.10.10:9000");
        conf.set("mapreduce.jobtracker.address", "172.20.10.10:54311");
        //conf.set("mapreduce.map.class", "RatingMapper");
        //conf.set("mapreduce.reduce.class", "StatisticalReducer.MeanReducer");
    }

    /* Creating a new job based on the configuration */
    private Job createJob() throws IOException {
        /* Creating a new job based on the configuration */
        Job job = Job.getInstance(conf, "Product Review Analysis");
        job.setJarByClass(RatingMapper.class);
        job.setMapperClass(RatingMapper.class);
        job.setReducerClass(statisticalReducer);


        /* key/value of your reducer output */
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        return job;
    }

    /* This methods runs the Hadoop job on top of the HDFS with specified statistic function for Combiner & Reducer */
    public boolean runHadoopJob(){
        try{
            System.out.println("Starting to run the code on Hadoop...");
            String[] argsTemp = { "hdfs://172.20.10.10:9000/customerReview/input", "hdfs://172.20.10.10:9000/customerReview/output" };
            Job job;

            /* Creating a configuration */
            createConfig();

            /* Creating a job based on the configuration */
            job = createJob();

            FileInputFormat.addInputPath(job, new Path(argsTemp[0]));

            /* This deletes possible output paths to prevent job failures */
            fs = FileSystem.get(conf);
            Path out = new Path(argsTemp[1]);
            fs.delete(out, true);

            /* Finally set the empty out path */
            FileOutputFormat.setOutputPath(job, new Path(argsTemp[1]));

            /* Running the job */
            job.submit();
            job.waitForCompletion(true);


            System.out.println("Job Finished!");
        } catch (Exception e) {
            System.out.println("Job Failed!");
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    /* Returns the result of MapReduce job as hashmap of key-value pairs */
    public HashMap<String,Double> getResults() throws IOException {

        HashMap<String,Double> jobResults = new HashMap<String, Double>();

        FileStatus[] fss = fs.listStatus(new Path("hdfs://172.20.10.10:9000/customerReview/output"));
        for (FileStatus status : fss) {
            Path path = status.getPath();
            System.out.println(path.getName());

            /* Reading only the reducer output files */
            if (path.getName().compareTo("_SUCCESS") != 0) {
                SequenceFile.Reader reader = new SequenceFile.Reader(conf, SequenceFile.Reader.file(path));
                Text key = new Text();
                DoubleWritable value = new DoubleWritable();

                /* Iterating through key-value pairs */
                while (reader.next(key, value)) {
                    //System.out.println(key.toString() + " | " + value.get());
                    jobResults.put(key.toString(),value.get());

                }
                reader.close();
            }

        }

        return jobResults;
    }

}
