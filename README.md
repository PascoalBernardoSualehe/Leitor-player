Leitor de Música para Android
Leitor de Música é uma aplicação Android completa, desenvolvida em Java, que oferece uma experiência robusta para ouvir e gerir músicas locais. A aplicação permite aos utilizadores explorar ficheiros de áudio do dispositivo, construir e guardar playlists personalizadas, e identificar músicas ambiente através da integração com a API da ACRCloud.
O projeto foi construído com foco na persistência de dados, garantindo que a fila de reprodução e as playlists do utilizador sejam guardadas e restauradas entre sessões de uso.

Funcionalidades Principais
Leitor de Música Completo:  Carrega e reproduz todas as músicas locais (.mp3, etc.) do dispositivo.  

Controles de Reprodução Avançados:
 Play, pause, avançar e retroceder.
 Modo aleatório (`shuffle`) que reordena a fila de reprodução de forma inteligente.
 Modo de repetição de uma única música.

Gestão de Playlists (Persistente):
   Criação de novas playlists.
   Visualização e edição de playlists existentes (PlaylistDetailActivity).
   Adição de qualquer música a uma ou mais playlists.
   Remoção de músicas de uma playlist.
   As playlists são guardadas numa base de dados local `SQLite` através do `Room`.

Fila de Reprodução Persistente: A fila de reprodução atual, a música que está a tocar e os modos (aleatório/repetição) são guardados automaticamente quando a aplicação é fechada e restaurados quando é reaberta.

Serviço em Primeiro Plano (`Foreground Service`):
O MusicService - garante que a música continue a tocar mesmo com a aplicação em segundo plano ou com o ecrã desligado.
 Exibe uma notificação de multimédia com a capa do álbum e controles de reprodução (anterior, play/pause, próximo).

Reconhecimento de Música (Estilo Shazam):
 Integração com o ACRCloud SDK.
 Um botão dedicado na tela principal permite identificar músicas que estão a tocar no ambiente, usando o microfone do dispositivo.
   
 Interface de Utilizador Intuitiva:
 MainActivity -exibe a música atual com uma capa de álbum rotativa e a lista da fila de reprodução.
 SongListActivity- mostra a lista completa de músicas disponíveis, com funcionalidade de pesquisa.
PlaylistsActivity e PlaylistDetailActivity para uma gestão completa das playlists.
A música em reprodução é destacada visualmente em todas as listas.

Gestão de Foco de Áudio: Pausa a música automaticamente quando outra aplicação (ex: YouTube, chamada telefónica) necessita de reproduzir som e retoma quando o foco de áudio é devolvido.

Tecnologias e Arquitetura
O projeto segue uma arquitetura inspirada no padrão Model-View-Presenter (MVP), utilizando tecnologias modernas do ecossistema Android.

Linguagem: Java
Arquitetura:
Model: Camada de dados representada pelas entidades `Room` (Playlist, Song, QueueState) e o PlaylistRepository.
View: As Activities (MainActivity, SongListActivity, etc.) e a interface definida em Contrato.java.
 Presenter: A classe `Presenter.java` que atua como intermediário, recebendo eventos da `View` e comunicando com o MusicService e o PlaylistRepository.


    Componentes Principais:
Android Jetpack Room: Usado para criar e gerir uma base de dados `SQLite` local de forma robusta. É responsável por guardar todas as playlists, as relações entre músicas e          playlists, e o estado da fila de reprodução.
Foreground Service (MusicService): O coração da aplicação, responsável por toda a lógica de reprodução de áudio com `MediaPlayer`, gestão da fila, modos de reprodução e notificações.
RecyclerView com ListAdapter: Utilizado para exibir todas as listas de músicas de forma eficiente, com atualizações de alta performance graças ao `DiffUtil`.
MediaSessionCompat`: Integra o leitor de música com o sistema Android, permitindo que seja controlado por dispositivos externos como smartwatches, Android Auto e auscultadores Bluetooth.
BroadcastReceiver:  Usado para a comunicação interna entre o MusicService e a MainActivity.

Bibliotecas Externas: 
ACRCloud SDK for Android: SDK em formato `.jar` para a funcionalidade de reconhecimento de música.
Glide : Para carregamento assíncrono e cache de imagens das capas dos álbuns.
Google GSON: Utilizado como `TypeConverter` no Room para serializar e desserializar a lista de músicas do estado da fila.
RxJava: Usado pelo Room para operações de base de dados reativas (ex: `Flowable`).
de.hdodenhof: circleimageview: Para a exibição circular da capa do álbum na tela principal.

📁 Estrutura do Projeto
com.example.leitor
├── 📂 Model/
│   ├── Musica.java           Modelo de dados principal para uma música.
│   └── QueueState.java       Entidade Room para guardar o estado da fila de reprodução.
│
├── 📂 data/
│   ├── AppDatabase.java      Configuração da base de dados Room.
│   ├── Playlist.java         Entidade Room para uma playlist.
│   ├── Song.java             Entidade Room para uma música (usada nas relações).
│   ├── PlaylistDao.java      Interface com todas as queries (SQL) para a base de dados.
│   ├── PlaylistRepository.java  Repositório que gere todas as operações de dados.
│   └── ...                   Outras classes de relação (CrossRef, WithSongs).
│
├── 📂 presenter/
│   └── Presenter.java        Presenter principal da aplicação.
│
├── 📂 view/
│   └── Contrato.java         Define as interfaces para a arquitetura MVP.
│
├── 📂 (raiz)
│   ├── MainActivity.java      Tela principal de reprodução.
│   ├── MusicService.java      Serviço de música em segundo plano.
│   ├── SongListActivity.java  Tela que exibe todas as músicas.
│   ├── PlaylistsActivity.java Tela que lista as playlists criadas.
│   ├── PlaylistDetailActivity.java  Tela que mostra as músicas de uma playlist específica.
│   ├── SongAdapter.java       Adapter universal para todas as RecyclerViews de música.
│   └── AudioRecognizer.java   Classe que encapsula a lógica do ACRCloud.











