package io.github.opendonationassistant.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = { VkClientTest.MOCK_SERVER_PORT })
public class VkClientTest {

  static final int MOCK_SERVER_PORT = 8080;

  private static final String CHANNEL_URL = "stcarolas";
  private static final String TOKEN = "Bearer access-token";

  private static final String CHANNEL_PATH = "/v1/channel";
  private static final String CHAT_PATH = "/v1/chat/message/send";
  private static final String CREATE_REWARD_PATH =
    "/v1/channel_point/reward/create";
  private static final String REJECT_REWARD_PATH =
    "/v1/channel_point/reward/demand/reject";

  private static final ObjectMapper mapper = ObjectMapper.getDefault();

  private static ApplicationContext context;
  private static VkClient client;

  private final ClientAndServer mockServer;

  public VkClientTest(ClientAndServer mockServer) {
    this.mockServer = mockServer;
  }

  @BeforeAll
  static void startClient() {
    context = ApplicationContext.builder(
      Map.of(
        "micronaut.http.services.vklive-data.url",
        "http://localhost:" + MOCK_SERVER_PORT
      )
    )
      .packages("io.github.opendonationassistant.integration")
      .start();
    client = new VkClient(context.getBean(VkDataClient.class), TOKEN);
  }

  @AfterAll
  static void stopClient() {
    if (context != null) {
      context.stop();
    }
  }

  @AfterEach
  void resetMockServer() {
    mockServer.reset();
  }

  @Test
  public void testSendsChatMessageToActiveStream() throws IOException {
    givenChannelInfo("""
      {"data":{"stream":{"id":"stream-1"}}}
      """);
    givenChatMessageAccepted("stream-1");

    assertNull(client.sendChatMessage(CHANNEL_URL, message()).join());

    var recorded = recordedRequests("POST", CHAT_PATH);
    assertEquals(1, recorded.length);
    assertEquals(TOKEN, recorded[0].getFirstHeader("Authorization"));
    assertEquals(
      Map.of("parts", List.of(Map.of("text", Map.of("content", "hello")))),
      body(recorded[0])
    );
  }

  @Test
  public void testFallsBackToFirstListedStream() throws IOException {
    givenChannelInfo("""
      {"data":{"streams":[{"id":"stream-2"}]}}
      """);
    givenChatMessageAccepted("stream-2");

    assertNull(client.sendChatMessage(CHANNEL_URL, message()).join());

    var recorded = recordedRequests("POST", CHAT_PATH);
    assertEquals(1, recorded.length);
    assertEquals(TOKEN, recorded[0].getFirstHeader("Authorization"));
    assertEquals(
      "stream-2",
      recorded[0].getFirstQueryStringParameter("stream_id")
    );
    assertEquals(
      Map.of("parts", List.of(Map.of("text", Map.of("content", "hello")))),
      body(recorded[0])
    );
  }

  @Test
  public void testFailsChatMessageWhenChannelIsOffline() {
    givenChannelInfo("""
      {"data":{"stream":null,"streams":null}}
      """);

    var future = client.sendChatMessage(CHANNEL_URL, message());

    assertThrows(CompletionException.class, future::join);
    assertNoChatMessageWasSent();
  }

  @Test
  public void testFailsChatMessageWhenChannelInfoReplyIsMissing() {
    mockServer
      .when(
        request()
          .withMethod("GET")
          .withPath(CHANNEL_PATH)
          .withQueryStringParameter("channel_url", CHANNEL_URL)
      )
      .respond(response().withStatusCode(204));

    var future = client.sendChatMessage(CHANNEL_URL, message());

    assertThrows(CompletionException.class, future::join);
    assertNoChatMessageWasSent();
  }

  @Test
  public void testFailsChatMessageWhenChannelInfoDataIsMissing() {
    givenChannelInfo("""
      {"data":null}
      """);

    var future = client.sendChatMessage(CHANNEL_URL, message());

    assertThrows(CompletionException.class, future::join);
    assertNoChatMessageWasSent();
  }

  @Test
  public void testRejectsReward() throws IOException {
    var request = new VkDataClient.DemandsRequest(
      List.of(new VkDataClient.Demand(8433849L))
    );
    mockServer
      .when(
        request()
          .withMethod("POST")
          .withPath(REJECT_REWARD_PATH)
          .withQueryStringParameter("channel_url", CHANNEL_URL)
      )
      .respond(response().withStatusCode(200));

    assertNull(client.rejectReward(CHANNEL_URL, request).join());

    var recorded = recordedRequests("POST", REJECT_REWARD_PATH);
    assertEquals(1, recorded.length);
    assertEquals(TOKEN, recorded[0].getFirstHeader("Authorization"));
    assertEquals(
      Map.of("demands", List.of(Map.of("id", 8433849))),
      body(recorded[0])
    );
  }

  @Test
  public void testCreatesRewardWithAccountToken() throws IOException {
    var request = new VkDataClient.RewardRequest(
      new VkDataClient.RewardRequest.Reward(true, "title", 100)
    );
    mockServer
      .when(
        request()
          .withMethod("POST")
          .withPath(CREATE_REWARD_PATH)
          .withQueryStringParameter("channel_url", CHANNEL_URL)
      )
      .respond(
        response()
          .withStatusCode(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {"data":{"reward":{"id":"reward-id"}}}
            """
          )
      );

    assertEquals("reward-id", client.createReward(CHANNEL_URL, request).join());

    var recorded = recordedRequests("POST", CREATE_REWARD_PATH);
    assertEquals(1, recorded.length);
    assertEquals(TOKEN, recorded[0].getFirstHeader("Authorization"));
    assertEquals(
      Map.of(
        "reward",
        Map.of("is_message_required", true, "name", "title", "price", 100)
      ),
      body(recorded[0])
    );
  }

  private void givenChannelInfo(String json) {
    mockServer
      .when(
        request()
          .withMethod("GET")
          .withPath(CHANNEL_PATH)
          .withQueryStringParameter("channel_url", CHANNEL_URL)
      )
      .respond(
        response()
          .withStatusCode(200)
          .withHeader("Content-Type", "application/json")
          .withBody(json)
      );
  }

  private void givenChatMessageAccepted(String streamId) {
    mockServer
      .when(
        request()
          .withMethod("POST")
          .withPath(CHAT_PATH)
          .withQueryStringParameter("channel_url", CHANNEL_URL)
          .withQueryStringParameter("stream_id", streamId)
      )
      .respond(response().withStatusCode(200));
  }

  private void assertNoChatMessageWasSent() {
    assertEquals(0, recordedRequests("POST", CHAT_PATH).length);
  }

  private HttpRequest[] recordedRequests(String method, String path) {
    return mockServer.retrieveRecordedRequests(
      request().withMethod(method).withPath(path)
    );
  }

  private static Map<String, Object> body(HttpRequest request)
    throws IOException {
    return Objects.requireNonNull(
      mapper.readValue(
        request.getBodyAsString(),
        Argument.mapOf(String.class, Object.class)
      )
    );
  }

  private static VkDataClient.ChatMessageRequest message() {
    return new VkDataClient.ChatMessageRequest(
      List.of(new VkDataClient.ChatPart(null, new VkDataClient.Text("hello")))
    );
  }
}
