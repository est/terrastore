/**
 * Copyright 2009 - 2010 Sergio Bossa (sergio.bossa@gmail.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package terrastore.server.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import terrastore.common.ClusterStats;
import terrastore.common.ErrorLogger;
import terrastore.common.ErrorMessage;
import terrastore.communication.CommunicationException;
import terrastore.server.Buckets;
import terrastore.server.Parameters;
import terrastore.server.Server;
import terrastore.server.ServerOperationException;
import terrastore.server.Values;
import terrastore.server.impl.cors.CorsController;
import terrastore.server.impl.cors.CorsInterceptor;
import terrastore.server.impl.support.JsonBucketsProvider;
import terrastore.server.impl.support.JsonClusterStatsProvider;
import terrastore.server.impl.support.JsonErrorMessageProvider;
import terrastore.server.impl.support.JsonParametersProvider;
import terrastore.server.impl.support.JsonServerOperationExceptionMapper;
import terrastore.server.impl.support.JsonValueProvider;
import terrastore.server.impl.support.JsonValuesProvider;
import terrastore.service.BackupOperationException;
import terrastore.service.BackupService;
import terrastore.service.QueryOperationException;
import terrastore.service.QueryService;
import terrastore.service.StatsService;
import terrastore.service.UpdateOperationException;
import terrastore.service.UpdateService;
import terrastore.store.Key;
import terrastore.store.Value;
import terrastore.store.features.Predicate;
import terrastore.store.features.Update;
import terrastore.store.features.Range;

/**
 * {@link terrastore.server.Server} implementation running a json-over-http server.
 *
 * @author Sergio Bossa
 */
@Path("/")
public class JsonHttpServer implements Server {

    public final static String HTTP_THREADS_CONFIGURATION_PARAMETER = "configuration.httpThreads";
    public final static String CORS_ALLOWED_ORIGINS_CONFIGURATION_PARAMETER = "configuration.corsAllowedOrigins";
    //
    private static final Logger LOG = LoggerFactory.getLogger(JsonHttpServer.class);
    //
    private final UpdateService updateService;
    private final QueryService queryService;
    private final BackupService backupService;
    private final StatsService statsService;
    //
    private org.mortbay.jetty.Server jetty;

    public JsonHttpServer(UpdateService updateService, QueryService queryService, BackupService backupService, StatsService statsService) {
        this.updateService = updateService;
        this.queryService = queryService;
        this.backupService = backupService;
        this.statsService = statsService;
    }

    @Override
    public synchronized void start(String host, int port, Map<String, String> configuration) throws Exception {
        ResteasyDeployment deployment = new ResteasyDeployment();
        registerProviders(deployment, configuration);
        registerResources(deployment, configuration);
        startServer(host, port, deployment, configuration);
    }

    @Override
    public synchronized void stop() throws Exception {
        jetty.stop();
    }

    @DELETE
    @Path("/{bucket}")
    public void removeBucket(@PathParam("bucket") String bucket) throws ServerOperationException {
        try {
            LOG.info("Removing bucket {}", bucket);
            updateService.removeBucket(bucket);
        } catch (CommunicationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        } catch (UpdateOperationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        }
    }

    @PUT
    @Path("/{bucket}/{key}")
    @Consumes("application/json")
    public void putValue(@PathParam("bucket") String bucket, @PathParam("key") Key key, Value value, @QueryParam("predicate") String predicate) throws ServerOperationException {
        try {
            LOG.info("Putting value with key {} to bucket {}", key, bucket);
            updateService.putValue(bucket, key, value, new Predicate(predicate));
        } catch (CommunicationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        } catch (UpdateOperationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        }
    }

    @DELETE
    @Path("/{bucket}/{key}")
    public void removeValue(@PathParam("bucket") String bucket, @PathParam("key") Key key) throws ServerOperationException {
        try {
            LOG.info("Removing value with key {} from bucket {}", key, bucket);
            updateService.removeValue(bucket, key);
        } catch (CommunicationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        } catch (UpdateOperationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        }
    }

    @POST
    @Path("/{bucket}/{key}/update")
    @Consumes("application/json")
    @Produces("application/json")
    public Value updateValue(@PathParam("bucket") String bucket, @PathParam("key") Key key, @QueryParam("function") String function, @QueryParam("timeout") Long timeout, Parameters parameters) throws ServerOperationException {
        try {
            if (function == null) {
                ErrorMessage error = new ErrorMessage(ErrorMessage.BAD_REQUEST_ERROR_CODE, "No update function provided!");
                throw new ServerOperationException(error);
            } else if (timeout == null) {
                ErrorMessage error = new ErrorMessage(ErrorMessage.BAD_REQUEST_ERROR_CODE, "No update timeout provided!");
                throw new ServerOperationException(error);
            }
            LOG.info("Updating value with key {} and function {} from bucket {}", new Object[]{key, function, bucket});
            Update update = new Update(function, timeout, parameters);
            return updateService.updateValue(bucket, key, update);
        } catch (CommunicationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        } catch (UpdateOperationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        }
    }

    @GET
    @Path("/")
    @Produces("application/json")
    public Buckets getBuckets() throws ServerOperationException {
        try {
            LOG.info("Getting buckets.");
            return new Buckets(queryService.getBuckets());
        } catch (CommunicationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        } catch (QueryOperationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        }
    }

    @GET
    @Path("/{bucket}/{key}")
    @Produces("application/json")
    public Value getValue(@PathParam("bucket") String bucket, @PathParam("key") Key key, @QueryParam("predicate") String predicate) throws ServerOperationException {
        try {
            LOG.info("Getting value with key {} from bucket {}", key, bucket);
            return queryService.getValue(bucket, key, new Predicate(predicate));
        } catch (CommunicationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        } catch (QueryOperationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        }
    }

    @GET
    @Path("/{bucket}")
    @Produces("application/json")
    public Values getAllValues(@PathParam("bucket") String bucket, @QueryParam("limit") int limit) throws ServerOperationException {
        try {
            LOG.info("Getting all values from bucket {}", bucket);
            return new Values(queryService.getAllValues(bucket, limit));
        } catch (CommunicationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        } catch (QueryOperationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        }
    }

    @GET
    @Path("/{bucket}/range")
    @Produces("application/json")
    public Values queryByRange(@PathParam("bucket") String bucket, @QueryParam("startKey") Key startKey, @QueryParam("endKey") Key endKey, @QueryParam("limit") int limit, @QueryParam("comparator") String comparator, @QueryParam("predicate") String predicateExpression, @QueryParam("timeToLive") long timeToLive) throws ServerOperationException {
        try {
            if (startKey == null) {
                ErrorMessage error = new ErrorMessage(ErrorMessage.BAD_REQUEST_ERROR_CODE, "No startKey provided!");
                throw new ServerOperationException(error);
            }
            if (comparator == null) {
                comparator = "";
            }
            LOG.info("Executing range query from {} to {} ordered by {} on bucket {}", new Object[]{startKey, endKey, comparator, bucket});
            Range range = new Range(startKey, endKey, limit, comparator);
            Predicate predicate = new Predicate(predicateExpression);
            return new Values(
                    queryService.queryByRange(bucket,
                    range,
                    predicate,
                    timeToLive));
        } catch (CommunicationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        } catch (QueryOperationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        }
    }

    @GET
    @Path("/{bucket}/predicate")
    @Produces("application/json")
    public Values queryByPredicate(@PathParam("bucket") String bucket, @QueryParam("predicate") String predicateExpression) throws ServerOperationException {
        try {
            if (predicateExpression == null) {
                ErrorMessage error = new ErrorMessage(ErrorMessage.BAD_REQUEST_ERROR_CODE, "No predicate provided!");
                throw new ServerOperationException(error);
            }
            LOG.info("Executing predicate query {} on bucket {}", predicateExpression, bucket);
            Predicate predicate = new Predicate(predicateExpression);
            return new Values(queryService.queryByPredicate(bucket, predicate));
        } catch (CommunicationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        } catch (QueryOperationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        }
    }

    @POST
    @Path("/{bucket}/import")
    public void importBackup(@PathParam("bucket") String bucket, @QueryParam("source") String source, @QueryParam("secret") String secret) throws ServerOperationException {
        try {
            if (source == null) {
                ErrorMessage error = new ErrorMessage(ErrorMessage.BAD_REQUEST_ERROR_CODE, "No source provided!");
                throw new ServerOperationException(error);
            } else if (secret == null) {
                ErrorMessage error = new ErrorMessage(ErrorMessage.BAD_REQUEST_ERROR_CODE, "No secret provided!");
                throw new ServerOperationException(error);
            }
            LOG.info("Importing backup for bucket {} from {}", bucket, source);
            backupService.importBackup(bucket, source, secret);
        } catch (CommunicationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        } catch (BackupOperationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        }
    }

    @POST
    @Path("/{bucket}/export")
    public void exportBackup(@PathParam("bucket") String bucket, @QueryParam("destination") String destination, @QueryParam("secret") String secret) throws ServerOperationException {
        try {
            if (destination == null) {
                ErrorMessage error = new ErrorMessage(ErrorMessage.BAD_REQUEST_ERROR_CODE, "No destination provided!");
                throw new ServerOperationException(error);
            } else if (secret == null) {
                ErrorMessage error = new ErrorMessage(ErrorMessage.BAD_REQUEST_ERROR_CODE, "No secret provided!");
                throw new ServerOperationException(error);
            }
            LOG.info("Exporting backup for bucket {} to {}", bucket, destination);
            backupService.exportBackup(bucket, destination, secret);
        } catch (CommunicationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        } catch (BackupOperationException ex) {
            ErrorMessage error = ex.getErrorMessage();
            ErrorLogger.LOG(LOG, error, ex);
            throw new ServerOperationException(error);
        }
    }

    @GET
    @Path("/_stats/cluster")
    @Produces("application/json")
    public ClusterStats getClusterStats() {
        LOG.info("Getting cluster statistics.");
        return statsService.getClusterStats();
    }

    public UpdateService getUpdateService() {
        return updateService;
    }

    public QueryService getQueryService() {
        return queryService;
    }

    @Override
    public BackupService getBackupService() {
        return backupService;
    }

    private void registerProviders(ResteasyDeployment deployment, Map<String, String> configuration) {
        List providers = Arrays.asList(
                new JsonBucketsProvider(),
                new JsonValuesProvider(),
                new JsonValueProvider(),
                new JsonClusterStatsProvider(),
                new JsonErrorMessageProvider(),
                new JsonParametersProvider(),
                new JsonServerOperationExceptionMapper(),
                new StringTextStar(),
                new CorsInterceptor(
                configuration.get(CORS_ALLOWED_ORIGINS_CONFIGURATION_PARAMETER),
                "POST, GET, PUT, DELETE, OPTIONS",
                "CONTENT-TYPE",
                "86400"));
        deployment.setProviders(providers);
    }

    private void registerResources(ResteasyDeployment deployment, Map<String, String> configuration) {
        List resources = Arrays.asList(this, new CorsController());
        deployment.setResources(resources);
    }

    private void startServer(String host, int port, ResteasyDeployment deployment, Map<String, String> configuration) throws Exception {
        jetty = new org.mortbay.jetty.Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        QueuedThreadPool threadPool = new QueuedThreadPool();
        Context context = new Context(jetty, "/", Context.NO_SESSIONS);
        context.setAttribute(ResteasyDeployment.class.getName(), deployment);
        context.addServlet(new ServletHolder(new HttpServletDispatcher()), "/*");
        connector.setHost(host);
        connector.setPort(port);
        threadPool.setMaxThreads(Integer.parseInt(configuration.get(HTTP_THREADS_CONFIGURATION_PARAMETER)));
        jetty.setConnectors(new Connector[]{connector});
        jetty.setThreadPool(threadPool);
        jetty.setGracefulShutdown(500);
        jetty.setStopAtShutdown(true);
        jetty.start();
    }
}
