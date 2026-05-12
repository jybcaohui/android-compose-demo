为当前 Android 项目封装一套生产级网络请求框架。

技术栈要求：
- Kotlin
- Jetpack Compose 项目
- Retrofit2
- OkHttp
- Gson 或 Kotlin Serialization（二选一，推荐 Gson）
- Coroutines
- Flow
- MVVM 架构
- Repository Pattern

目标：
构建一套适用于金融/信贷类 App 的通用网络层，具备：
- 易扩展
- 易维护
- 支持统一错误处理
- 支持 Token 管理
- 支持日志打印
- 支持请求重试
- 支持多环境切换
- 支持 Loading/Error/Success 状态管理

项目结构要求：

network/
├── api/
├── model/
├── interceptor/
├── response/
├── repository/
├── manager/
├── exception/
├── state/

需要生成以下内容：

1. RetrofitManager
   要求：
- 单例封装
- 配置 BaseUrl
- 配置 GsonConverterFactory
- 配置 Coroutine 支持
- 配置超时时间
- 支持 Debug 日志
- 支持动态 Header
- 支持 Token 自动注入

2. OkHttp Interceptors
   包含：
- HeaderInterceptor
- AuthInterceptor
- LoggingInterceptor
- RetryInterceptor

功能：
- 自动添加 Authorization Token
- 自动添加设备信息
- 自动添加 App Version
- Debug 环境打印完整请求日志
- 网络失败自动重试

3. ApiService
   生成示例接口：
- login
- getUserInfo
- submitKyc
- getCreditResult

要求：
- 使用 suspend
- 使用 Retrofit annotations
- POST/GET 示例都包含

4. BaseResponse<T>
   统一响应结构：

{
"code": 200,
"message": "success",
"data": {}
}

要求：
- 泛型支持
- 支持空 data
- 支持错误码处理

5. NetworkResult
   封装网络状态：

- Success
- Error
- Loading
- Empty

使用 sealed class 实现。

6. SafeApiCall
   封装统一请求入口：

功能：
- try-catch
- HttpException 处理
- SocketTimeoutException 处理
- IOException 处理
- JSON 解析异常处理
- 自动转换为 NetworkResult

7. Repository 示例
   例如：
- AuthRepository
- UserRepository

要求：
- 调用 ApiService
- 返回 Flow<NetworkResult<T>>

8. ViewModel 示例
   要求：
- 使用 StateFlow
- 管理 Loading/Error/Success
- 调用 Repository

9. TokenManager
   功能：
- 保存 Token
- 获取 Token
- 清除 Token
- 使用 DataStore 实现

10. EnvironmentManager
    支持：
- DEV
- TEST
- PROD

要求：
- 可动态切换 BaseUrl
- Debug 环境默认 DEV

11. 网络错误码统一处理
    例如：
- 701 -> 登录失效
- 403 -> 无权限
- 500 -> 服务器异常
- 网络超时
- 无网络

生成统一 ErrorMessageMapper。

12. Compose 页面调用示例
    示例：
- LoginScreen
- 点击按钮发起请求
- 展示 Loading
- 展示错误信息
- 展示成功结果

代码要求：
- 所有代码可直接运行
- 使用最新稳定写法
- 不使用过时 API
- 添加必要注释
- 避免 Experimental API
- 符合生产级代码规范
- 不省略关键实现
- 所有 import 必须完整

额外要求：
- 网络层解耦
- 方便后续替换接口
- 方便后续接入 Hilt/Koin
- 后续方便扩展文件上传、下载、分页

最后输出：
1. 完整目录结构
2. Gradle 依赖
3. 所有 Kotlin 文件完整代码
4. 示例接口调用流程
5. 最佳实践说明