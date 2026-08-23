# Privacidad de ubicación — ClimaAMO 0.2.1

ClimaAMO no solicita ubicación al iniciar. El permiso de ubicación se pide únicamente cuando el usuario toca el botón **Aquí**. Las coordenadas se usan para consultar el pronóstico de esa ubicación y, cuando Android lo permite, convertirlas a un nombre legible mediante el geocodificador del sistema. No se requiere cuenta y ClimaAMO no mantiene un perfil del usuario.

Las consultas meteorológicas se realizan por HTTPS a Open-Meteo y, para comparación meteorológica, a MET Norway. MET Norway requiere identificación de la aplicación mediante User-Agent y recomienda limitar las coordenadas a cuatro decimales; ClimaAMO cumple ese límite en esa fuente.
