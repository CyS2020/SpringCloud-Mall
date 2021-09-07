package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallThirdPartyApplicationTests {

	@Autowired
	private OSSClient ossClient;

	@Test
	public void contextLoads() {
	}

	@Test
	public void testUpLoad() throws FileNotFoundException {
		// yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-hangzhou.aliyuncs.com。
		//String endpoint = "oss-cn-shanghai.aliyuncs.com";
		// 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
		//String accessKeyId = "LTAI5tPYRTnjTRPrpWbv3qXR";
		//String accessKeySecret = "j6CJbUGPLkUc004B9xb5VH6At1SAqE";

		// 创建OSSClient实例。
		//OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

		// 填写本地文件的完整路径。如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件流。
		InputStream inputStream = new FileInputStream("C:\\Users\\Administrator\\Pictures\\许昕2.jpg");
		// 依次填写Bucket名称（例如examplebucket）和Object完整路径（例如exampledir/exampleobject.txt）。Object完整路径中不能包含Bucket名称。
		ossClient.putObject("cys-gulimall", "haha.jpg", inputStream);
		System.out.println("上传完成...");
		// 关闭OSSClient。
		ossClient.shutdown();
	}
}
