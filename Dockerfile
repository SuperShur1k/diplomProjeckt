# Базовый образ Java
FROM openjdk:17-jdk-slim

# Устанавливаем информацию о разработчике
LABEL authors="SuperShurik"

# Устанавливаем рабочую директорию
WORKDIR /telegrma-bot-nails

# Копируем сборку из папки проекта
COPY target/telegramBotNailsBooking-0.0.1-SNAPSHOT.jar telegrma-bot-nails.jar

# Передаем переменную окружения (токен бота)
ENV BOT_TOKEN=7620720012:AAGXBdShOB8tjtSMdbNLkbx0bQd0LElVkMs

# Указываем команду для запуска приложения
ENTRYPOINT ["java", "-jar", "telegrma-bot-nails.jar"]
