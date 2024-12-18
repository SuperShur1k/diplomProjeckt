package com.example.telegramBotNailsBooking;

import com.example.telegramBotNailsBooking.model.Users;
import com.example.telegramBotNailsBooking.repository.UserRepository;
import com.example.telegramBotNailsBooking.service.AdminService;
import com.example.telegramBotNailsBooking.service.MessageService;
import com.example.telegramBotNailsBooking.service.UserService;
import com.example.telegramBotNailsBooking.service.buttons.AdminButtons;
import com.example.telegramBotNailsBooking.service.buttons.AutUserButtons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

import static org.mockito.Mockito.*;

class TelegramBotNailsBookingApplicationTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private MessageService messageService;

	@Mock
	private AdminButtons adminButtons;

	@Mock
	private AutUserButtons autUserButtons;

	@InjectMocks
	private AdminService adminService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

//	@Test
//	void testShowAdminPanel() {
//		Long chatId = 123456789L;
//		String languageCode = "en";
//
//		// Mock user repository to return the language code
//		when(userRepository.findLanguageCodeByChatId(chatId)).thenReturn(languageCode);
//
//		// Мокируем клавиатуру
//		InlineKeyboardMarkup mockKeyboard = new InlineKeyboardMarkup(); // Инициализация пустой клавиатуры
//
//		// Мокируем возвращаемое значение метода getAdminInlineKeyboard
//		when(adminButtons.getAdminInlineKeyboard(chatId)).thenReturn(mockKeyboard);
//
//		// Call the method
//		adminService.showAdminPanel(chatId);
//
//		// Verify interaction with messageService
//		verify(messageService, times(1)).sendLocalizedMessageWithInlineKeyboard(
//				eq(chatId),
//				eq("admin.panel.welcome"),
//				eq(languageCode),
//				eq(mockKeyboard)  // Убедитесь, что передаем мокированную клавиатуру
//		);
//	}

//	@Test
//	void testSetAdmin_Success() {
//		Long chatId = 123456789L;
//		String phone = "1234567890";
//		String languageCode = "en";
//		Users user = new Users();
//		user.setPhoneNumber(phone);
//		user.setFirstName("John");
//		user.setLastName("Doe");
//
//		// Mock user repository to return the user
//		when(userRepository.findLanguageCodeByChatId(chatId)).thenReturn(languageCode);
//		when(userRepository.findByPhoneNumber(phone)).thenReturn(user);
//
//		// Создаем кнопку и клавиатуру для теста
//		InlineKeyboardButton button = new InlineKeyboardButton();
//		button.setText("Mock Button");
//		button.setCallbackData("/mock");
//
//		InlineKeyboardMarkup mockKeyboard = new InlineKeyboardMarkup();
//		mockKeyboard.setKeyboard(Collections.singletonList(Collections.singletonList(button)));
//
//		// Мокируем метод getAdminInlineKeyboard для возврата мокированного значения
//		when(adminButtons.getAdminInlineKeyboard(chatId)).thenReturn(mockKeyboard);
//
//		// Call the method
//		adminService.setAdmin(chatId, phone);
//
//		// Verify that the user's role is updated to ADMIN and saved
//		verify(userRepository, times(1)).save(user);
//
//		// Verify that the correct success message is sent with the correct keyboard
//		verify(messageService, times(1)).sendLocalizedMessageWithInlineKeyboard(
//				eq(chatId),
//				eq("admin.user.successfully.granted"),
//				eq(languageCode),
//				eq(mockKeyboard),  // Убедитесь, что передаем правильную клавиатуру
//				eq(user.getFirstName()),
//				eq(user.getLastName())
//		);
//	}

//	@Test
//	void testInitiateSetAdmin() {
//		Long chatId = 123456789L;
//		String languageCode = "en";
//
//		// Mock user repository to return the language code
//		when(userRepository.findLanguageCodeByChatId(chatId)).thenReturn(languageCode);
//
//		// Мокируем клавиатуру, которая возвращается из autUserButtons.getCancelInlineKeyboard
//		InlineKeyboardButton button = new InlineKeyboardButton();
//		button.setText("Cancel Operation");
//		button.setCallbackData("/cancel");
//
//		InlineKeyboardMarkup mockKeyboard = new InlineKeyboardMarkup();
//		mockKeyboard.setKeyboard(Collections.singletonList(Collections.singletonList(button)));
//
//		when(autUserButtons.getCancelInlineKeyboard(chatId)).thenReturn(mockKeyboard);
//
//		// Call the method
//		adminService.initiateSetAdmin(chatId);
//
//		// Verify that the correct messages were sent
//		verify(messageService, times(1)).sendLocalizedMessage(eq(chatId), eq("set.admin.enter.phone"), eq(languageCode));
//		verify(messageService, times(1)).sendLocalizedMessageWithInlineKeyboard(eq(chatId), eq("set.admin.cancel.operation"), eq(languageCode), eq(mockKeyboard));
//	}

	@Test
	void testInitiateDelAdmin_NoAdmins() {
		Long chatId = 123456789L;
		String languageCode = "en";

		// Мокируем возвращение языка пользователя
		when(userRepository.findLanguageCodeByChatId(chatId)).thenReturn(languageCode);
		when(userRepository.findAll()).thenReturn(Collections.emptyList());

		// Мокируем метод adminButtons.getAdminInlineKeyboard для возврата клавиатуры
		InlineKeyboardButton button = new InlineKeyboardButton();
		button.setText("No Admins");
		button.setCallbackData("/no_admins");

		InlineKeyboardMarkup mockKeyboard = new InlineKeyboardMarkup();
		mockKeyboard.setKeyboard(Collections.singletonList(Collections.singletonList(button)));

		when(adminButtons.getAdminInlineKeyboard(chatId)).thenReturn(mockKeyboard);

		// Вызываем метод
		adminService.initiateDelAdmin(chatId);

		// Проверяем, что отправлено сообщение с правильной клавиатурой
		verify(messageService, times(1)).sendLocalizedMessageWithInlineKeyboard(
				eq(chatId),
				eq("admin.remove.no.admins"),
				eq(languageCode),
				eq(mockKeyboard)  // Передаем мокиированную клавиатуру
		);
	}

	@Test
	void testRemoveAdminById_AdminNotFound() {
		Long chatId = 123456789L;
		Long adminId = 1L;
		String languageCode = "en";

		// Mock user repository to return the language code and no admin found
		when(userRepository.findLanguageCodeByChatId(chatId)).thenReturn(languageCode);
		when(userRepository.findById(adminId)).thenReturn(Optional.empty());

		// Мокируем клавиатуру, которая возвращается из adminButtons.getAdminInlineKeyboard
		InlineKeyboardMarkup mockKeyboard = new InlineKeyboardMarkup();
		when(adminButtons.getAdminInlineKeyboard(chatId)).thenReturn(mockKeyboard);

		// Call the method
		adminService.removeAdminById(chatId, adminId);

		// Verify that the correct "admin not found" message is sent with the correct keyboard
		verify(messageService, times(1)).sendLocalizedMessageWithInlineKeyboard(
				eq(chatId),
				eq("admin.not.found.or.removed"),
				eq(languageCode),
				eq(mockKeyboard)  // Убедитесь, что передаем клавиатуру, полученную через adminButtons
		);
	}

	@Test
	void testRemoveAdminById_Success() {
		Long chatId = 123456789L;
		Long adminId = 1L;
		String languageCode = "en";
		Users admin = new Users();
		admin.setId(adminId);
		admin.setFirstName("Admin");
		admin.setLastName("User");
		admin.setRole(Users.Role.ADMIN);

		// Mock user repository to return the admin
		when(userRepository.findLanguageCodeByChatId(chatId)).thenReturn(languageCode);
		when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

		// Мокируем клавиатуру, которая возвращается из adminButtons.getAdminInlineKeyboard
		InlineKeyboardMarkup mockKeyboard = new InlineKeyboardMarkup();
		when(adminButtons.getAdminInlineKeyboard(chatId)).thenReturn(mockKeyboard);

		// Call the method
		adminService.removeAdminById(chatId, adminId);

		// Verify that the admin's role is changed to CLIENT and saved
		verify(userRepository, times(1)).save(admin);

		// Verify that the success message is sent with the correct keyboard
		verify(messageService, times(1)).sendLocalizedMessageWithInlineKeyboard(
				eq(chatId),
				eq("admin.role.removed.success"),
				eq(languageCode),
				eq(mockKeyboard),  // Убедитесь, что передаем клавиатуру, полученную через adminButtons
				eq(admin.getFirstName()),
				eq(admin.getLastName())
		);
	}

//	SO HARDDDDDDDDD
}
