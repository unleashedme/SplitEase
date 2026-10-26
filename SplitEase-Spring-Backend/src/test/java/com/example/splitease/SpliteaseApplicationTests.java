package com.example.splitease;

import com.example.splitease.service.NotificationService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SpliteaseApplicationTests {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private NotificationService notificationService;

	@Test
	void contextLoads() {
	}

	private void register(String name, String email, String phone) throws Exception {
		mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"phone\":\"" + phone + "\",\"password\":\"pw-" + name + "\"}"))
				.andExpect(status().isOk());
	}

	private String login(String name, String email) throws Exception {
		String body = mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"pw-" + name + "\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(body, "$.token");
	}

	@Test
	void protectedEndpointsRequireAuthentication() throws Exception {
		mvc.perform(get("/dashboard")).andExpect(status().isUnauthorized());
		mvc.perform(get("/dashboard").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());
	}

	@Test
	void wrongPasswordIsRejected() throws Exception {
		register("Zed", "zed@flow.com", "500");
		mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"zed@flow.com\",\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void fullSplitAndSettleFlow() throws Exception {
		register("Ann", "ann@flow.com", "100");
		register("Ben", "ben@flow.com", "101");
		String ann = login("Ann", "ann@flow.com");
		String ben = login("Ben", "ben@flow.com");

		String created = mvc.perform(post("/group").header("Authorization", ann).contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Flat\",\"members\":[\"ben@flow.com\"]}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String groupId = JsonPath.read(created, "$.groupId");

		mvc.perform(post("/expense").header("Authorization", ann).contentType(MediaType.APPLICATION_JSON)
						.content("{\"groupId\":\"" + groupId + "\",\"amount\":100.00,\"description\":\"Groceries\"}"))
				.andExpect(status().isOk());

		mvc.perform(get("/dashboard").header("Authorization", ann))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.amountOwedToUser").value(50.0))
				.andExpect(jsonPath("$.amountUserOwes").value(0));

		mvc.perform(get("/dashboard").header("Authorization", ben))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.amountUserOwes").value(50.0));

		mvc.perform(get("/group").header("Authorization", ben))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalGroups").value(1))
				.andExpect(jsonPath("$.activeGroupsCount").value(1))
				.andExpect(jsonPath("$.groups[0].expenses[0].description").value("Groceries"));

		mvc.perform(get("/activity").header("Authorization", ben))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalExpensesCount").value(1))
				.andExpect(jsonPath("$.expenseHistory[0].owesToName").value("Ann"));

		String payables = mvc.perform(get("/settlements/payables").header("Authorization", ben))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].toUserName").value("Ann"))
				.andExpect(jsonPath("$[0].amount").value(50.0))
				.andReturn().getResponse().getContentAsString();
		String annId = JsonPath.read(payables, "$[0].toUserId");

		mvc.perform(post("/settlements").header("Authorization", ben).contentType(MediaType.APPLICATION_JSON)
						.content("{\"toUserId\":\"" + annId + "\",\"amount\":50.00,\"note\":\"upi\"}"))
				.andExpect(status().isOk());

		mvc.perform(get("/settlements/payables").header("Authorization", ben))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
		mvc.perform(get("/dashboard").header("Authorization", ann))
				.andExpect(jsonPath("$.amountOwedToUser").value(0));
		mvc.perform(get("/group").header("Authorization", ann))
				.andExpect(jsonPath("$.activeGroupsCount").value(0));

		// Only the creator may delete the group
		mvc.perform(delete("/group/{id}", groupId).header("Authorization", ben))
				.andExpect(status().isForbidden());
		mvc.perform(delete("/group/{id}", groupId).header("Authorization", ann))
				.andExpect(status().isOk());
		mvc.perform(get("/myGroups").header("Authorization", ann))
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void billPhotoFlow() throws Exception {
		register("Cat", "cat@flow.com", "200");
		register("Dan", "dan@flow.com", "201");
		register("Eve", "eve@flow.com", "202");
		String cat = login("Cat", "cat@flow.com");
		String dan = login("Dan", "dan@flow.com");
		String eve = login("Eve", "eve@flow.com");

		String created = mvc.perform(post("/group").header("Authorization", cat).contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Cafe\",\"members\":[\"dan@flow.com\"]}"))
				.andReturn().getResponse().getContentAsString();
		String groupId = JsonPath.read(created, "$.groupId");

		String expense = mvc.perform(post("/expense").header("Authorization", cat).contentType(MediaType.APPLICATION_JSON)
						.content("{\"groupId\":\"" + groupId + "\",\"amount\":450.00,\"description\":\"Coffee\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String expenseId = JsonPath.read(expense, "$.expenseId");

		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(600, 800, BufferedImage.TYPE_INT_RGB), "png", png);
		MockMultipartFile file = new MockMultipartFile("file", "bill.png", "image/png", png.toByteArray());

		// Only the payer can attach the bill
		mvc.perform(multipart("/expense/{id}/receipt", expenseId).file(file).header("Authorization", dan))
				.andExpect(status().isForbidden());
		mvc.perform(multipart("/expense/{id}/receipt", expenseId).file(file).header("Authorization", cat))
				.andExpect(status().isOk());

		mvc.perform(get("/group").header("Authorization", dan))
				.andExpect(jsonPath("$.groups[0].expenses[0].expenseId").value(expenseId))
				.andExpect(jsonPath("$.groups[0].expenses[0].hasReceipt").value(true));
		mvc.perform(get("/activity").header("Authorization", dan))
				.andExpect(jsonPath("$.expenseHistory[0].hasReceipt").value(true));

		// Everyone in the group can see it, people outside it can't
		mvc.perform(get("/expense/{id}/receipt", expenseId).header("Authorization", dan))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/png"))
				.andExpect(content().bytes(png.toByteArray()));
		mvc.perform(get("/expense/{id}/receipt", expenseId).param("size", "thumb").header("Authorization", dan))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/jpeg"));
		mvc.perform(get("/expense/{id}/receipt", expenseId).header("Authorization", eve))
				.andExpect(status().isForbidden());

		mvc.perform(delete("/expense/{id}/receipt", expenseId).header("Authorization", cat))
				.andExpect(status().isOk());
		mvc.perform(get("/expense/{id}/receipt", expenseId).header("Authorization", dan))
				.andExpect(status().isNotFound());
		mvc.perform(get("/activity").header("Authorization", dan))
				.andExpect(jsonPath("$.expenseHistory[0].hasReceipt").value(false));
	}
}
