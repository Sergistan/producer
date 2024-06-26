package com.utochkin.kafkaproducerforsma.services;

import com.utochkin.kafkaproducerforsma.dto.ChatDto;
import com.utochkin.kafkaproducerforsma.dto.MessageDto;
import com.utochkin.kafkaproducerforsma.exceptions.AccessDeniedException;
import com.utochkin.kafkaproducerforsma.exceptions.BadInputDataException;
import com.utochkin.kafkaproducerforsma.mappers.ChatMapper;
import com.utochkin.kafkaproducerforsma.mappers.MessageMapper;
import com.utochkin.kafkaproducerforsma.models.Chat;
import com.utochkin.kafkaproducerforsma.models.Message;
import com.utochkin.kafkaproducerforsma.models.Role;
import com.utochkin.kafkaproducerforsma.models.User;
import com.utochkin.kafkaproducerforsma.repository.ChatRepository;
import com.utochkin.kafkaproducerforsma.repository.MessageRepository;
import com.utochkin.kafkaproducerforsma.repository.UserRepository;
import com.utochkin.kafkaproducerforsma.services.impl.ChatServiceImpl;
import com.utochkin.kafkaproducerforsma.services.interfaces.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @InjectMocks
    private ChatServiceImpl chatService;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ChatMapper chatMapper;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void getChatById() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(Collections.emptySet())
                .followers(Collections.emptySet())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .password(passwordEncoder.encode("222"))
                .email("dzaga73i98@gmail.com")
                .role(Role.ROLE_USER)
                .friends(Collections.emptySet())
                .followers(Collections.emptySet())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .users(Set.of(user1, user2))
                .lastMessage("Msg")
                .build();

        ChatDto chatDto = new ChatDto(1L, null, "Msg", user1.getId(), user2.getId(), Set.of(user1, user2));

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Assertions.assertTrue(chat.getUsers().contains(user1));

        when(chatMapper.toDto(chat)).thenReturn(chatDto);

        when(chatService.getLastMessage(chat.getId())).thenReturn(chat.getLastMessage());
        chatDto.setLastMessage(chat.getLastMessage());

        Assertions.assertEquals(chatDto, chatService.getChatById(chat.getId()));
    }

    @Test
    void getChatByIdAccessDenied() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(Collections.emptySet())
                .followers(Collections.emptySet())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .users(new HashSet<>())
                .lastMessage("Msg")
                .build();

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Assertions.assertThrows(AccessDeniedException.class, () -> chatService.getChatById(chat.getId()));
    }

    @Test
    void createChat() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(Collections.emptySet())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .password(passwordEncoder.encode("222"))
                .email("dzaga73i98@gmail.com")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(Collections.emptySet())
                .build();

        user1.addFriend(user2);
        user2.addFriend(user1);

        ChatDto chatDto = new ChatDto(1L, null, "Msg", user1.getId(), user2.getId(), null);

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userService.getById(chatDto.getFirstUserId())).thenReturn(user1);
        when(userService.getById(chatDto.getSecondUserId())).thenReturn(user2);

        Assertions.assertEquals(user1.getFriends(), Set.of(user2));
        Assertions.assertFalse(user1.getChats().stream().anyMatch(chat -> user2.getChats().contains(chat)));

        Chat chat = new Chat(1L, LocalDateTime.now(), "Msg", Set.of(user1, user2), Collections.emptyList());

        when(chatMapper.toChat(chatDto)).thenReturn(chat);

        chatRepository.save(chat);

        ChatDto newChatDto = new ChatDto(1L, chat.getCreatedAt(), "Msg", user1.getId(), user2.getId(), Set.of(user1, user2));

        when(chatMapper.toDto(chat)).thenReturn(newChatDto);

        Assertions.assertEquals(newChatDto, chatService.createChat(chatDto));
    }

    @Test
    void createChatAccessDenied() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .friends(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .friends(new HashSet<>())
                .build();

        User user3 = User.builder()
                .id(3L)
                .name("Alina")
                .build();

        ChatDto chatDto = new ChatDto(1L, null, "Msg", user1.getId(), user2.getId(), null);

        user1.addFriend(user2);
        user2.addFriend(user1);

        doReturn(user3.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userService.getById(chatDto.getFirstUserId())).thenReturn(user1);
        when(userService.getById(chatDto.getSecondUserId())).thenReturn(user2);

        Assertions.assertThrows(AccessDeniedException.class, () -> chatService.createChat(chatDto));
    }

    @Test
    void createChatDontFriend() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .friends(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .friends(new HashSet<>())
                .build();

        ChatDto chatDto = new ChatDto(1L, null, "Msg", user1.getId(), user2.getId(), null);

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userService.getById(chatDto.getFirstUserId())).thenReturn(user1);
        when(userService.getById(chatDto.getSecondUserId())).thenReturn(user2);

        Assertions.assertThrows(BadInputDataException.class, () -> chatService.createChat(chatDto));
    }

    @Test
    void createChatAlreadyHaveChat() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .users(new HashSet<>())
                .lastMessage("Msg")
                .users(Set.of(user1, user2))
                .build();

        user1.addFriend(user2);
        user2.addFriend(user1);
        user1.setChats(Set.of(chat));
        user2.setChats(Set.of(chat));

        ChatDto chatDto = new ChatDto(1L, null, "Msg", user1.getId(), user2.getId(), null);

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userService.getById(chatDto.getFirstUserId())).thenReturn(user1);
        when(userService.getById(chatDto.getSecondUserId())).thenReturn(user2);

        Assertions.assertThrows(BadInputDataException.class, () -> chatService.createChat(chatDto));
    }

    @Test
    void deleteChatById() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .password(passwordEncoder.encode("222"))
                .email("dzaga73i98@gmail.com")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(Set.of(user1, user2))
                .messages(Collections.emptyList())
                .build();

        user1.addFriend(user2);
        user2.addFriend(user1);
        user1.setChats(Set.of(chat));
        user2.setChats(Set.of(chat));

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Assertions.assertFalse(chat.getUsers().stream().map(User::getName).noneMatch(x -> x.equals(user1.getName())));

        chatService.deleteChatById(chat.getId());

        verify(chatRepository, times(1)).deleteById(chat.getId());
    }

    @Test
    void deleteChatByIdAccessDenied() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .password(passwordEncoder.encode("222"))
                .email("dzaga73i98@gmail.com")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(Set.of(user2))
                .messages(Collections.emptyList())
                .build();

        user1.addFriend(user2);
        user2.addFriend(user1);
        user2.setChats(Set.of(chat));

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Assertions.assertThrows(AccessDeniedException.class, () -> chatService.deleteChatById(chat.getId()));
    }

    @Test
    void joinChat() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .password(passwordEncoder.encode("222"))
                .email("dzaga73i98@gmail.com")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(new HashSet<>())
                .messages(Collections.emptyList())
                .build();

        user1.addFriend(user2);
        user2.addFriend(user1);
        user2.setChats(Set.of(chat));
        chat.setUsers(Set.of(user2));

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByName(user1.getName())).thenReturn(Optional.of(user1));
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Assertions.assertTrue(chat.getUsers().size() != 2);
        Assertions.assertTrue(user2.getFriends().contains(user1));

        Set<User> usersOfChat = new HashSet<>();
        usersOfChat.add(user1);
        chat.setUsers(usersOfChat);

        Set<User> usersOfChat1 = new HashSet<>();
        usersOfChat1.add(user2);
        chat.setUsers(usersOfChat1);

        Assertions.assertEquals(user1.getId(), chatService.joinChat(chat.getId()));
        verify(chatRepository, times(1)).save(chat);
    }

    @Test
    void joinChatAlreadyHaveThisChat() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .password(passwordEncoder.encode("222"))
                .email("dzaga73i98@gmail.com")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(new HashSet<>())
                .messages(Collections.emptyList())
                .build();

        user1.addFriend(user2);
        user2.addFriend(user1);
        user1.setChats(Set.of(chat));
        user2.setChats(Set.of(chat));
        chat.setUsers(Set.of(user1, user2));

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByName(user1.getName())).thenReturn(Optional.of(user1));
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        Assertions.assertThrows(BadInputDataException.class, () -> chatService.joinChat(chat.getId()));
    }

    @Test
    void joinChatUserCantJoinInThisChat() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .password(passwordEncoder.encode("222"))
                .email("dzaga73i98@gmail.com")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(new HashSet<>())
                .messages(Collections.emptyList())
                .build();

        user1.setChats(Set.of(chat));
        chat.setUsers(Set.of(user1));

        doReturn(user2.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByName(user2.getName())).thenReturn(Optional.of(user2));
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        Assertions.assertThrows(AccessDeniedException.class, () -> chatService.joinChat(chat.getId()));
    }

    @Test
    void leaveChat() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .password(passwordEncoder.encode("222"))
                .email("dzaga73i98@gmail.com")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(new HashSet<>())
                .messages(Collections.emptyList())
                .build();

        user1.addFriend(user2);
        user2.addFriend(user1);

        Set<User> userSet = new HashSet<>();
        userSet.add(user1);
        userSet.add(user2);
        chat.setUsers(userSet);

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByName(user1.getName())).thenReturn(Optional.of(user1));
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Set<User> usersOfChat = new HashSet<>();
        usersOfChat.add(user1);
        usersOfChat.add(user2);
        chat.setUsers(usersOfChat);

        Assertions.assertTrue(chat.getUsers().contains(user1));

        Assertions.assertEquals(user1.getId(), chatService.leaveChat(chat.getId()));
        verify(chatRepository, times(1)).save(chat);
        verify(chatRepository, never()).deleteById(chat.getId());
    }

    @Test
    void leaveChatAccessDenied() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .password(passwordEncoder.encode("222"))
                .email("dzaga73i98@gmail.com")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(Set.of(user1))
                .messages(Collections.emptyList())
                .build();


        doReturn(user2.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByName(user2.getName())).thenReturn(Optional.of(user2));
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Assertions.assertThrows(AccessDeniedException.class, () -> chatService.leaveChat(chat.getId()));
    }

    @Test
    void leaveChatWhenChatIsEmpty() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .password(passwordEncoder.encode("222"))
                .email("dzaga73i98@gmail.com")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(new HashSet<>())
                .messages(Collections.emptyList())
                .build();

        user1.addFriend(user2);
        user2.addFriend(user1);

        Set<User> userSet = new HashSet<>();
        userSet.add(user1);
        chat.setUsers(userSet);

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByName(user1.getName())).thenReturn(Optional.of(user1));
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Assertions.assertEquals(user1.getId(), chatService.leaveChat(chat.getId()));
        verify(chatRepository, times(1)).deleteById(chat.getId());
        verify(chatRepository, never()).save(chat);
    }

    @Test
    void getLastMessage() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(Set.of(user1))
                .messages(Collections.emptyList())
                .build();

        user1.setChats(Set.of(chat));

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Assertions.assertTrue(chat.getUsers().stream().map(User::getName).anyMatch(x -> x.equals(user1.getName())));

        when(messageRepository.getLastMessageFromChat(chat.getId())).thenReturn(chat.getLastMessage());

        Assertions.assertEquals(chat.getLastMessage(), chatService.getLastMessage(chat.getId()));
    }

    @Test
    void getLastMessageAccessDenied() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(Set.of(user2))
                .messages(Collections.emptyList())
                .build();

        user2.setChats(Set.of(chat));

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Assertions.assertThrows(AccessDeniedException.class, () -> chatService.getLastMessage(chat.getId()));
    }

    @Test
    void getAllMessagesInChat() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .password(passwordEncoder.encode("111"))
                .email("sergistan.utochkin@yandex.ru")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Message msg1 = new Message(1L, "1 MSG", LocalDateTime.now(), user1, null);
        Message msg2 = new Message(2L, "2 MSG", LocalDateTime.now(), user1, null);

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(Set.of(user1))
                .messages(List.of(msg1, msg2))
                .build();

        user1.setChats(Set.of(chat));
        msg1.setChat(chat);
        msg2.setChat(chat);

        MessageDto msg1Dto = new MessageDto(1L, 1L, "1 MSG", msg1.getSender().getName(), msg1.getSentAt());
        MessageDto msg2Dto = new MessageDto(2L, 1L, "2 MSG", msg2.getSender().getName(), msg2.getSentAt());

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Assertions.assertTrue(chat.getUsers().stream().map(User::getName).anyMatch(x -> x.equals(user1.getName())));

        when(messageRepository.getAllMessagesInChat(chat.getId())).thenReturn(chat.getMessages());

        when(messageMapper.toListDto(chat.getMessages())).thenReturn(List.of(msg1Dto, msg2Dto));

        Assertions.assertEquals(List.of(msg1Dto, msg2Dto), chatService.getAllMessagesInChat(chat.getId()));
    }

    @Test
    void getAllMessagesInChatAccessDenied() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .chats(new HashSet<>())
                .build();

        Chat chat = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg")
                .users(Set.of(user2))
                .messages(Collections.emptyList())
                .build();

        user2.setChats(Set.of(chat));

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Assertions.assertThrows(AccessDeniedException.class, () -> chatService.getAllMessagesInChat(chat.getId()));
    }

    @Test
    void getAllChats() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Ilya")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user3 = User.builder()
                .id(3L)
                .name("Alina")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        User user4 = User.builder()
                .id(4L)
                .name("Tom")
                .role(Role.ROLE_ADMIN)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat1 = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg1")
                .users(Set.of(user1, user2))
                .messages(Collections.emptyList())
                .build();

        Chat chat2 = Chat.builder()
                .id(2L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg2")
                .users(Set.of(user1, user3))
                .messages(Collections.emptyList())
                .build();

        user1.addFriend(user2);
        user2.addFriend(user1);
        user1.addFriend(user3);
        user3.addFriend(user1);
        user1.setChats(Set.of(chat1));
        user2.setChats(Set.of(chat1));
        user1.setChats(Set.of(chat2));
        user3.setChats(Set.of(chat2));

        ChatDto chatDto1 = new ChatDto(1L, chat1.getCreatedAt(), "Msg1", user1.getId(), user2.getId(), Set.of(user1, user2));
        ChatDto chatDto2 = new ChatDto(1L, chat2.getCreatedAt(), "Msg2", user1.getId(), user3.getId(), Set.of(user1, user3));

        doReturn(user4.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.getAllNameAdmins()).thenReturn(List.of(user4.getName()));

        when(chatRepository.findAll()).thenReturn(List.of(chat1, chat2));

        when(chatMapper.toListDto(List.of(chat1, chat2))).thenReturn(List.of(chatDto1, chatDto2));

        Assertions.assertEquals(List.of(chatDto1, chatDto2), chatService.getAllChats());
    }

    @Test
    void getAllChatsAccessDenied() {
        User user1 = User.builder()
                .id(1L)
                .name("Sergey")
                .role(Role.ROLE_USER)
                .friends(new HashSet<>())
                .chats(new HashSet<>())
                .build();

        Chat chat1 = Chat.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .lastMessage("Msg1")
                .users(Set.of(user1))
                .messages(Collections.emptyList())
                .build();


        user1.setChats(Set.of(chat1));

        doReturn(user1.getName()).when(authentication).getName();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.getAllNameAdmins()).thenReturn(List.of("Tom"));

        Assertions.assertThrows(AccessDeniedException.class, () -> chatService.getAllChats());
    }
}