package com.foo.rpc.examples.spring.scheduled;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.rpc.examples.spring.SpringController;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.api.dto.MockDatabaseDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ExecutionStatusDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.MockRPCExternalServiceDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ScheduleTaskInvocationResultDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCTestDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.*;

public class OrderFulfillmentController extends SpringController {

    private static final ObjectMapper mapper = new ObjectMapper();

    private OrderFulfillmentService.Client client;

    public OrderFulfillmentController() {
        super(OrderFulfillmentApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        String iface = OrderFulfillmentService.Iface.class.getName();
        return new RPCProblem(
                new HashMap<String, Object>() {{
                    put(iface, client);
                }},
                new HashMap<String, List<String>>() {{
                    put(iface, Collections.singletonList("backdoor"));
                }},
                null,
                null,
                null,
                RPCType.GENERAL
        );
    }

    @Override
    public String startClient() {
        String url = "http://localhost:" + getSutPort() + "/order-fulfillment";
        try {
            TTransport transport = new THttpClient(url);
            TProtocol protocol = new TBinaryProtocol(transport);
            client = new OrderFulfillmentService.Client(protocol);
        } catch (TTransportException e) {
            e.printStackTrace();
        }

        return url;
    }

    @Override
    public void resetStateOfSUT() {
        try {
            client.backdoor(null, null, null);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<SeededRPCTestDto> seedRPCTests() {
        return Arrays.asList(
                seededOrderTest("test_priority_order_approved",
                        "order-priority", true, "order-priority", 5, true, true),
                seededOrderTest("test_standard_order_approved",
                        "order-standard", true, "order-standard", 2, false, true),
                seededOrderTest("test_out_of_stock",
                        "order-empty", true, "order-empty", 0, false, true),
                seededOrderTest("test_payment_declined",
                        "order-declined", false, "order-declined", 3, false, true),
                seededOrderTest("test_pending_refresh",
                        "order-pending", true, "order-pending", 3, false, false)
        );
    }

    private SeededRPCTestDto seededOrderTest(String testName,
                                             String orderId,
                                             boolean paymentApproved,
                                             String inventoryOrderId,
                                             int stock,
                                             boolean priorityCustomer,
                                             boolean invokeRefreshTask) {
        return new SeededRPCTestDto() {{
            this.testName = testName;
            rpcFunctions = Collections.singletonList(new SeededRPCActionDto() {{
                interfaceName = OrderFulfillmentService.Iface.class.getName();
                functionName = "evaluateOrder";
                inputParams = Collections.singletonList(orderId);
                inputParamTypes = Collections.singletonList(String.class.getName());
                mockRPCExternalServiceDtos = Collections.singletonList(new MockRPCExternalServiceDto() {{
                    appKey = "order.fulfillment";
                    interfaceFullName = PaymentGateway.class.getName();
                    functionName = "authorize";
                    responses = Collections.singletonList("{\"orderId\":\"" + orderId + "\",\"approved\":" + paymentApproved + "}");
                    responseTypes = Collections.singletonList(PaymentAuthorization.class.getName());
                }});
                mockDatabaseDtos = Collections.singletonList(new MockDatabaseDto() {{
                    appKey = "order.fulfillment";
                    commandName = "Inventory.Table";
                    response = "{\"orderId\":\"" + inventoryOrderId + "\",\"stock\":" + stock + ",\"priorityCustomer\":" + priorityCustomer + "}";
                    responseFullType = InventoryRow.class.getName();
                }});
            }});

            if (invokeRefreshTask) {
                scheduleTaskInvocations = Collections.singletonList(new ScheduleTaskInvocationDto() {{
                    appKey = "order.fulfillment";
                    taskName = "refreshInventorySnapshot";
                    descriptiveInfo = "Refreshes the inventory snapshot used by evaluateOrder";
                }});
            }
        }};
    }

    @Override
    public boolean customizeMockingRPCExternalService(List<MockRPCExternalServiceDto> externalServiceDtos, boolean enabled) {
        try {
            if (!enabled) {
                return client.backdoor(null, null, null);
            }

            boolean ok = true;
            for (MockRPCExternalServiceDto dto : externalServiceDtos) {
                if (dto.responses == null || dto.responses.isEmpty()) {
                    continue;
                }
                JsonNode json = mapper.readTree(dto.responses.get(0));
                PaymentAuthorization authorization = new PaymentAuthorization();
                authorization.orderId = json.get("orderId").asText();
                authorization.approved = json.get("approved").asBoolean();
                ok = ok && client.backdoor(authorization, null, null);
            }
            return ok;
        } catch (JsonProcessingException | TException e) {
            return false;
        }
    }

    @Override
    public boolean customizeMockingDatabase(List<MockDatabaseDto> databaseDtos, boolean enabled) {
        try {
            if (!enabled) {
                return client.backdoor(null, null, null);
            }

            boolean ok = true;
            for (MockDatabaseDto dto : databaseDtos) {
                if (dto.response == null) {
                    continue;
                }
                JsonNode json = mapper.readTree(dto.response);
                InventoryRow row = new InventoryRow();
                row.orderId = json.get("orderId").asText();
                row.stock = json.get("stock").asInt();
                row.priorityCustomer = json.get("priorityCustomer").asBoolean();
                ok = ok && client.backdoor(null, row, null);
            }
            return ok;
        } catch (JsonProcessingException | TException e) {
            return false;
        }
    }

    @Override
    public ScheduleTaskInvocationResultDto customizeScheduleTaskInvocation(ScheduleTaskInvocationDto invocationDto, boolean invoked) {
        ScheduleTaskInvocationResultDto dto = new ScheduleTaskInvocationResultDto();
        dto.status = ExecutionStatusDto.FAILED;
        dto.taskName = invocationDto.taskName;

        try {
            if (invocationDto != null && invoked) {
                InventoryRefreshTask task = new InventoryRefreshTask();
                task.taskName = invocationDto.taskName;
                boolean ok = client.backdoor(null, null, task);
                dto.status = ok ? ExecutionStatusDto.COMPLETED : ExecutionStatusDto.FAILED;
                dto.invocationId = invocationDto.taskName;
            }
        } catch (TException e) {
            dto.status = ExecutionStatusDto.FAILED;
        }

        return dto;
    }

    @Override
    public boolean isScheduleTaskCompleted(ScheduleTaskInvocationResultDto invocationInfo) {
        return true;
    }
}
