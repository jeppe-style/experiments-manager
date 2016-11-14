package cloud.benchflow.experimentsmanager.testutils;

import cloud.benchflow.experimentsmanager.db.DbManager;
import cloud.benchflow.experimentsmanager.db.ExperimentsDAO;
import cloud.benchflow.experimentsmanager.db.entities.Experiment;
import cloud.benchflow.experimentsmanager.manager.AsyncRunExperiment;
import cloud.benchflow.experimentsmanager.utils.DriversMaker;
import cloud.benchflow.faban.client.FabanClient;
import cloud.benchflow.faban.client.exceptions.RunIdNotFoundException;
import cloud.benchflow.faban.client.responses.RunId;
import cloud.benchflow.minio.BenchFlowMinioClient;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.mockito.mock.MockName;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

/**
 * Created by Jesper Findahl (jesper.findahl@usi.ch) on 07.11.16.
 */
public class SetUpMocks {

    public static Answer testAnswer = invocationOnMock -> {

        Object[] args = invocationOnMock.getArguments();
        Object mock = invocationOnMock.getMock();
        MockName name = MockUtil.getMockName(mock);

        System.out.println("mocking: " + name + "." + invocationOnMock.getMethod().getName());
        return null;
    };

    public static void setUpMinioClientMocks(BenchFlowMinioClient minioClientMock) throws IOException {

        Mockito.doAnswer(testAnswer)
                .when(minioClientMock)
                .saveOriginalDeploymentDescriptor(Mockito.anyString(), Mockito.anyString());

        Mockito.doAnswer(testAnswer)
                .when(minioClientMock)
                .saveOriginalTestConfiguration(Mockito.anyString(), Mockito.anyString());

        Mockito.doAnswer(testAnswer)
                .when(minioClientMock)
                .saveModel(Mockito.anyString(), Mockito.anyString(), Mockito.any(InputStream.class));

//        Mockito.doAnswer(testAnswer)
//                .when(minioClientMock)
//                .removeModels(Mockito.anyString());

    }


    public static void setUpMinioClientMocksWithExperiment(BenchFlowMinioClient minioClientMock, Experiment experiment) throws IOException {


        Mockito.doAnswer(testAnswer)
                .when(minioClientMock)
                .saveOriginalDeploymentDescriptor(Mockito.eq(experiment.getMinioExperimentId()), Mockito.anyString());

        Mockito.doAnswer(testAnswer)
                .when(minioClientMock)
                .saveOriginalTestConfiguration(Mockito.eq(experiment.getMinioExperimentId()), Mockito.anyString());

        Mockito.doAnswer(testAnswer)
                .when(minioClientMock)
                .saveModel(Mockito.eq(experiment.getMinioExperimentId()), Mockito.anyString(), Mockito.any(InputStream.class));

        Mockito.doAnswer(testAnswer)
                .when(minioClientMock)
                .removeModels(Mockito.eq(experiment.getMinioExperimentId()));

    }


    public static void setUpDbManagerMocks(DbManager dbMock) throws IOException {

        SessionFactory mockedSessionFactory = Mockito.mock(SessionFactory.class);
        Session mockedSession = Mockito.mock(Session.class);
        Transaction mockedTransaction = Mockito.mock(Transaction.class);

        Mockito.when(dbMock.getExperimentsDAO())
                .thenReturn(new ExperimentsDAO(mockedSessionFactory));

        Mockito.when(mockedSession.beginTransaction())
                .thenReturn(mockedTransaction);

        Mockito.when(mockedSessionFactory.getCurrentSession())
                .thenReturn(mockedSession);


        // TODO - mock DbManager
//        Mockito.doAnswer(testAnswer)
//                .when(dbMock)
//                .getExperimentsDAO().saveExperiment(Mockito.any(Experiment.class));


    }

    public static void setUpFabanClientMocks(FabanClient fabanMock) throws RunIdNotFoundException {

        // TODO - mock Faban Client
        Mockito.doAnswer(testAnswer)
                .when(fabanMock)
                .status(Mockito.any(RunId.class));


    }

    public static void setUpDriversMakerMock(DriversMaker driversMakerMock) {

        // TODO - mock Drivers Maker Mock
        Mockito.doAnswer(testAnswer)
                .when(driversMakerMock)
                .generateBenchmark(Mockito.anyString(), Mockito.anyLong(), Mockito.anyInt());


    }

    public static void setUpRunExperimentsPoolMock(ExecutorService runExperimentsPoolMock) {

        Mockito.when(runExperimentsPoolMock.submit(Mockito.any(AsyncRunExperiment.class)))
                .thenReturn(null);

    }

}
