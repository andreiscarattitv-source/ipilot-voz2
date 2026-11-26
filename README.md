# i-Pilot Voz (v2.0)

App Android que ouve comandos de voz (pt-BR) e "toca" nos botões do app i-Pilot (Minn Kota)
usando um Serviço de Acessibilidade. Não usa Bluetooth/API do motor: ele só aperta os botões na tela.

## Como gerar o APK
**Opção A – Android Studio:** File > Open (esta pasta) > aguarde o Gradle > Build > Build APK(s).
**Opção B – GitHub (sem instalar nada):** crie um repositório, envie estes arquivos, abra a aba Actions >
"Build APK" > Run workflow; baixe o artefato `ipilot-voz-apk` (app-debug.apk).

## Primeiro uso
1. Instale o APK (permita "fontes desconhecidas").
2. Abra o app > permitir microfone e notificações.
3. Ative "i-Pilot Voz" em Acessibilidade (Android pode exigir Configurações > Apps > i-Pilot Voz > ⋮ > "Permitir configurações restritas").
4. Teste a calibração dos botões; confira os "graus por toque" no seu motor.
5. Toque em INICIAR, abra o i-Pilot e fale: "piloto, direita 10".

## Comandos
- aumentar / diminuir velocidade (opcional: "aumentar velocidade 2")
- âncora
- direita / esquerda + graus (5, 10, 15, 20, 25 ... ou por extenso)
- ligar hélice / desligar (ou parar) hélice — o app fala "Hélice ligada" / "Hélice desligada"
- travar rumo (botão N)
- Âncora: o app fala "Âncora ativado". Com âncora ativa, qualquer novo comando desativa a âncora ("Âncora desativado") antes de executar.
- Um ponto verde aparece na tela onde cada toque é feito.
