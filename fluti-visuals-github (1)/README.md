# Fluti Visuals (Fabric, MC 1.21.11) — базовий каркас
1. Відкрий https://fabricmc.net/develop , вибери 1.21.11, скопіюй версії у gradle.properties (рядки з CHECK).
2. Додай gradle wrapper: `gradle wrapper` (або скопіюй з шаблону Fabric).
3. `./gradlew build` → build/libs/fluti-visuals-0.1.0.jar
4. Right Shift — меню. Клавішу можна змінити в Controls (наприклад, на Left Shift).
Конфіг: .minecraft/config/fluti/config.json ; свої звуки: config/fluti/sounds/hit.ogg, kill.ogg
Найбільш версійно-залежні місця (див. require=0): ClientWorldMixin, SkyRenderingMixin, ChatScreenMixin, MouseMixin.
