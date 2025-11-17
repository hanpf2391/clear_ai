package com.hanpf.clearai.tools.user;// package com.hanpf.n8nworkflowai.tools.user;
//
// import com.hanpf.langchain4jdome.enntity.User;
// import com.hanpf.langchain4jdome.server.UserService;
// import dev.langchain4j.agent.tool.P;
// import dev.langchain4j.agent.tool.Tool;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;
//
// /**
//  * @author hanpf
//  * @date 2025/8/3 上午5:28
//  */
// @Component
// public class UserTools {
//     @Autowired
//     private UserService userService;
//
//     // 1.添加用户工具：
//     @Tool("添加用户服务")
//     public String insertUser(
//             @P("用户姓名") String username,
//             @P("用户密码") String password,
//             @P("用户邮箱")String email,
//             @P("用户昵称")String nickname) {
//         try {
//             User user = new User();
//             user.setEmail(email);
//             user.setNickname(nickname);
//             user.setUsername(username);
//             user.setPassword(password);
//             userService.insertUser(user);
//             return String.format("用户添加成功！\n用户名: %s\n昵称: %s\n邮箱: %s",
//                     username, nickname, email);
//         } catch (Exception e) {
//             return "添加用户失败：" + e.getMessage();
//         }
//     }
//     // 2，根据用户昵称查询用户信息：
//     @Tool(name = "根据用户昵称查询用户的基础信息")
//     public String findByNickname(@P("用户昵称") String nickname) {
//         try {
//             User user = userService.findByNickname(nickname);
//             if (user != null) {
//                 return String.format("查询成功！用户信息：\n" +
//                         "ID: %d\n" +
//                         "用户名: %s\n" +
//                         "昵称: %s\n" +
//                         "邮箱: %s\n" +
//                         "状态: %s\n" +
//                         "创建时间: %s",
//                         user.getId(),
//                         user.getUsername(),
//                         user.getNickname(),
//                         user.getEmail(),
//                         user.getStatus() == 0 ? "正常" : "禁用",
//                         user.getCreateTime());
//             } else {
//                 return "未找到昵称为 '" + nickname + "' 的用户";
//             }
//         } catch (Exception e) {
//             return "查询用户时出现错误：" + e.getMessage();
//         }
//     }
// }
