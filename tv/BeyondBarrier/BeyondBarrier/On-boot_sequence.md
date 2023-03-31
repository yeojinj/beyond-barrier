기기 부팅 시 서비스 어플리케이션을 실행할 수 있도록 하는 방법
======
1. SDB 를 이용할 시 root 권한으로 설정
```
sdb root on
```

2. SDB 를 이용한 연결, 혹은 Device Manager 를 이용한다.
```
sdb connect "device IP"

sdb connect 70.12.115.41
```

3. /usr/lib/systemd/system/ 경로에 service 파일 생성 및 수정
```
vi /usr/lib/systemd/system/your-service.service

[Unit]
Description=Your Service
After=multi-user.target

[Service]
Type=simple
ExecStart=/opt/usr/apps/your-package-name/bin/your-service-binary
Restart=always

[Install]
WantedBy=multi-user.target

```

4. 이후 과정
```
systemctl daemon-reload
systemctl enable your-service.service
systemctl start your-service.service
systemctl status your-service.service
```

아마 될 것이다.