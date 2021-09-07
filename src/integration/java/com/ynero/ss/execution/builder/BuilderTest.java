package com.ynero.ss.execution.builder;

import com.ynero.ss.execution.domain.dto.EdgeDTO;
import com.ynero.ss.execution.domain.dto.NodeDTO;
import com.ynero.ss.execution.domain.dto.PipelineDTO;
import com.ynero.ss.execution.services.sl.NodeService;
import com.ynero.ss.execution.services.sl.PipelineService;
import com.ynero.ss.pipeline.dto.proto.PipelinesMessage;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@AutoConfigureDataMongo
@ActiveProfiles("integration-test")
@Testcontainers
@DirtiesContext
@Log4j2
public class BuilderTest {

    public static final String MONGO_VERSION = "4.4.4";

    @Autowired
    protected MongoOperations mongo;

    @Container
    protected static final MongoDBContainer MONGO_CONTAINER = new MongoDBContainer("mongo:" + MONGO_VERSION);

    @DynamicPropertySource
    protected static void mongoProperties(DynamicPropertyRegistry reg) {
        reg.add("spring.data.mongodb.uri", () -> {
            return MONGO_CONTAINER.getReplicaSetUrl();
        });
    }

    @AfterEach
    protected void cleanupAllDataInDb() {
        mongo.getCollectionNames().forEach(collection -> {
            mongo.remove(new Query(), collection);
        });
    }

    private List<String> nodeIds = new ArrayList<>();
    private String pipelineId;

    @Setter(onMethod_ = {@Autowired})
    private NodeService nodeService;

    @Setter(onMethod_ = {@Autowired})
    private PipelineService pipelineService;

    @Setter(onMethod_ = {@Autowired})
    private GraphBuilder graphBuilder;

    private PipelinesMessage.PipelineDevices pipelineDevices;

    private List<PipelinesMessage.DeviceData> deviceDataList = new ArrayList<>();

    @BeforeEach
    void setUp() {
        var dtos = List.of(
                NodeDTO.builder()
                        .script("def val = params[\"temperature-read\"]; Map<String, Object> result = new HashMap<>(); result.put(\"val\", val); return result;")
                        .inputPortsName(List.of("temperature-read"))
                        .outputPortsName(List.of("val"))
                        .build(),
                NodeDTO.builder()
                        .script("def val = params[\"val\"]; def K = params[\"K\"]; def C = val-K; Map<String, Object> result = new HashMap<>(); result.put(\"C\", C); return result;")
                        .inputPortsName(List.of("val", "k"))
                        .outputPortsName(List.of("C"))
                        .build(),
                NodeDTO.builder()
                        .script("Map<String, Object> result = new HashMap<>(); result.put(\"K\", 273.15); return result;")
                        .outputPortsName(List.of("K"))
                        .build(),
                NodeDTO.builder()
                        .script("def inp1 = params[\"input-1\"]; def inp2 = params[\"input-2\"]; def avg = (inp1+inp2)/2; Map<String, Object> result = new HashMap<>(); result.put(\"avg\", avg); return result;")
                        .inputPortsName(List.of("input-1", "input-2"))
                        .outputPortsName(List.of("avg"))
                        .build()
        );
        dtos.forEach(
                nodeDTO -> {
                    nodeIds.add(nodeService.save(nodeDTO));
                }
        );
        var edges = List.of(
                EdgeDTO.builder()
                        .nodeIdi(nodeIds.get(0))
                        .nodeIdj(nodeIds.get(1))
                        .outputPortNameOfNodeI("val")
                        .inputPortNameOfNodeJ("val")
                        .build(),
                EdgeDTO.builder()
                        .nodeIdi(nodeIds.get(2))
                        .nodeIdj(nodeIds.get(1))
                        .outputPortNameOfNodeI("K")
                        .inputPortNameOfNodeJ("K")
                        .build(),
                EdgeDTO.builder()
                        .nodeIdi(nodeIds.get(1))
                        .nodeIdj(nodeIds.get(3))
                        .outputPortNameOfNodeI("C")
                        .inputPortNameOfNodeJ("input-2")
                        .build(),
                EdgeDTO.builder()
                        .nodeIdi(nodeIds.get(0))
                        .nodeIdj(nodeIds.get(1))
                        .outputPortNameOfNodeI("val")
                        .inputPortNameOfNodeJ("val")
                        .build(),
                EdgeDTO.builder()
                        .nodeIdi(nodeIds.get(2))
                        .nodeIdj(nodeIds.get(1))
                        .outputPortNameOfNodeI("K")
                        .inputPortNameOfNodeJ("K")
                        .build(),
                EdgeDTO.builder()
                        .nodeIdi(nodeIds.get(1))
                        .nodeIdj(nodeIds.get(3))
                        .outputPortNameOfNodeI("C")
                        .inputPortNameOfNodeJ("input-1")
                        .build()
        );
        var pipelineDTO = PipelineDTO.builder()
                .tenantId("Volbeat")
                .edges(edges)
                .build();
        pipelineId = pipelineService.create(pipelineDTO);

        for (int i = 0; i < 2; i++) {
            deviceDataList.add(
                    PipelinesMessage.DeviceData.newBuilder()
                            .setDeviceId(UUID.randomUUID().toString())
                            .setPort("temperature-read")
                            .setValue(new BigDecimal(290+i).toString())
                            .build()
            );
        }

        pipelineDevices = PipelinesMessage.PipelineDevices.newBuilder()
                .setPipelineId(pipelineId)
                .addAllDevicesData(deviceDataList)
                .build();
    }

    @Test
    void output(){
        log.info(pipelineId);
        var pipeline = pipelineService.find(pipelineId);
        log.info(pipeline);
        graphBuilder.build(pipelineDevices);
    }

}