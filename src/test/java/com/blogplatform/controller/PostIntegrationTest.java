class PostIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;

    @Test
    void getPublishedPosts_returnsPagedResult() {
        var response = rest.getForEntity(
            "/api/posts?page=0&size=5", String.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).contains("\"success\":true");
    }

    @Test
    void createPost_withoutAuth_returns401() {
        var req = new PostDto.CreateRequest();
        req.setTitle("Test"); req.setContent("Body content.");
        var response = rest.postForEntity(
            "/api/posts", req, String.class);
        assertThat(response.getStatusCode())
            .isEqualTo(UNAUTHORIZED);
    }
}
