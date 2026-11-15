# Decisões de design

Um bloco por decisão: contexto, decisão, alternativa rejeitada, consequência.

## Fila de reservas: composição em vez de herança

**Contexto.** A reserva precisa respeitar a ordem de chegada, e a fila é por item.

**Decisão.** `FilaDeReservas` tem uma `List<Reserva>` privada e expõe só `entrar`, `remover`,
`ativas` (uma cópia), `primeiraSemExemplar`, `temReservaDe` e `posicaoDe`. Cada `Item` tem a sua fila.

**Alternativa rejeitada.** `FilaDeReservas extends ArrayList<Reserva>`: exporia `add(int, ...)`,
`set` e `clear`, e qualquer classe poderia furar a ordem de chegada.

**Consequência.** A ordem só muda por métodos da própria fila, e os testes da fila são os testes da
regra. Não há hierarquia criada só para "usar herança": não existe nenhuma neste modelo.

## GRASP 1 (Especialista na Informação): `Item` decide para onde vai um exemplar livre

**Contexto.** Quando um exemplar é devolvido, ou solto por uma reserva cancelada, alguém precisa
decidir se ele fica reservado para a próxima da fila ou volta à estante. Na primeira versão isso
seria um `if` dentro do serviço de empréstimos, lendo a fila e o status por fora.

**Decisão.** `Item.destinarExemplarLivre(exemplar)`: o item é quem tem a fila e os exemplares. O
mesmo raciocínio manda `Item.exemplaresDisponiveis()` (contar os exemplares é do item, e não um
campo guardado à parte), `Emprestimo.estaAtrasadoEm(data)` (o empréstimo tem as datas) e
`Exemplar.podeSerEmprestadoPara(usuario)` (o exemplar tem status e reserva dona).

**Alternativa rejeitada.** Deixar tudo em `EmprestimosEmMemoria`, que leria status, compararia datas
e consultaria filas: um serviço grande e um domínio anêmico.

**Consequência.** `devolver` e `cancelarReserva` usam a mesma regra, escrita uma vez. O serviço só
valida na ordem da especificação e orquestra.

## GRASP 2 (Especialista na Informação): `Usuario` conhece os próprios empréstimos

**Contexto.** `UsuarioView.emprestimosAtivos` precisa de valores reais, e `desativarUsuario` precisa
saber se há empréstimo ativo.

**Decisão.** `Usuario` guarda a lista dos seus `Emprestimo` e conta os ativos (`emprestimosAtivos()`).
`UsuariosEmMemoria` e `EmprestimosEmMemoria` perguntam ao usuário.

**Alternativa rejeitada.** O serviço de empréstimos contar os ativos de cada usuário quando
precisasse: menos acoplamento entre `Usuario` e `Emprestimo`, mas `UsuariosEmMemoria` passaria a
depender do serviço de empréstimos só para poder desativar alguém.

**Consequência.** `Usuario` e `Emprestimo` se conhecem. Em troca, o limite de empréstimos e a
desativação não dependem de nenhum outro serviço.

## Criador: o serviço cria `Emprestimo` e `Reserva`

**Decisão.** `EmprestimosEmMemoria` cria os dois, depois de todas as verificações, porque é ele quem
guarda os índices (empréstimos ativos por tombo, reservas por id) e tem o contador de ids. Nenhuma
classe de fora do pacote `servico` cria um `Emprestimo`: a Facade só devolve visões.

## Facade: `BibliotecaDaEquipe` é a porta única

**Contexto.** A interface `Biblioteca` do contrato já é uma Facade. A interface gráfica da Entrega 3
vai falar só com ela.

**Decisão.** `BibliotecaDaEquipe` implementa `Biblioteca` e é o único ponto de entrada (`Fabrica` só
sabe criá-la). Os serviços e o `RegistroDaBiblioteca` são package-private no pacote `servico`: quem
está fora recebe `ItemView`, `UsuarioView` etc., nunca um `Item`, uma coleção ou uma classe de arquivo.

**Alternativa rejeitada.** Serviços públicos que a interface gráfica instanciaria diretamente.

**Consequência.** Trocar a persistência ou uma coleção não aparece para quem usa o sistema.

## Um registro compartilhado, em vez de coleções em cada serviço

**Contexto.** Empréstimo precisa enxergar exemplares e usuários; `carregar()` precisa trocar o estado
inteiro sem deixar a biblioteca pela metade se o arquivo estiver corrompido.

**Decisão.** `RegistroDaBiblioteca` guarda todas as coleções (`Map<String, Item>`,
`Map<String, Exemplar>`, `Map<String, Usuario>`, a lista de empréstimos, o índice de empréstimos
ativos por tombo e o de reservas ativas por id) e os contadores de id, e tem os métodos
`exigirItem`, `exigirExemplar` e `exigirUsuario`. Os serviços não guardam coleção própria.

**Alternativa rejeitada.** Cada serviço com as próprias coleções e os serviços chamando uns aos
outros: o "localizar e lançar se não achar" seria copiado em cada operação, e o `carregar()` teria de
trocar o estado de três serviços em sequência.

**Consequência.** O código duplicado da Entrega 1 (localizar e lançar `RecursoNaoEncontradoException`
em três métodos) virou três métodos `exigir*` usados em toda parte, e `carregar()` troca tudo com
atribuições simples no fim, depois de ler e conferir tudo.

## Reservas de um usuário desativado

**Contexto.** A especificação deixa em aberto o que acontece com as reservas de quem é desativado.

**Decisão.** A reserva continua na fila. Se um exemplar for separado para ela, ele fica `RESERVADO`
até o usuário ser reativado (e emprestar) ou a reserva ser cancelada: um usuário inativo não pode
emprestar (`UsuarioInativoException`).

**Alternativa rejeitada.** Cancelar automaticamente as reservas ao desativar: faria `desativarUsuario`
mexer em exemplares e filas, e perderia a posição do usuário na fila se ele voltasse.

**Consequência.** Um exemplar pode ficar guardado para alguém inativo. Cancelar a reserva o libera.

## Persistência: uma tabela genérica de arquivo, e o domínio sem E/S

**Contexto.** Cinco arquivos (itens, exemplares, usuários, empréstimos, reservas) com a mesma forma:
linhas de campos de texto.

**Decisão.** Uma classe `ArquivoDeTabela`, que não conhece o domínio, grava e lê linhas de campos
(escapando `;`, `\` e quebras de linha, e escrevendo o nulo como `\N`), com `try-with-resources`,
`IOException` convertida em `PersistenciaException` com a causa (`initCause`) e arquivo ausente
tratado como vazio. `PersistenciaDaBiblioteca` converte entre domínio e linhas. Nenhuma classe do
pacote `dominio` faz E/S. O diretório vem de `DiretorioDeDados.atual()`, lido dentro de `salvar` e
`carregar`.

**Alternativa rejeitada.** Cinco classes quase idênticas, uma por arquivo: o mesmo código de
leitura e gravação cinco vezes.

**Consequência.** `carregar()` monta um registro novo ao lado, confere referências (item de um
exemplar, tombo e matrícula de um empréstimo, exemplar de uma reserva) e consistência
(`EMPRESTADO` precisa de empréstimo ativo; `RESERVADO` precisa de reserva dona), e só no fim troca o
estado atual. Os contadores de id continuam depois do maior id carregado. Os arquivos gerados
(`itens.txt` e os outros) não são versionados.

## Extensão autoral: multa calculada, e por fora do contrato

**Contexto.** A regra própria da equipe é cobrar multa por atraso no acervo de livros. O contrato
promete o prazo de 14 dias e o comportamento de `emprestar` e `devolver`: a extensão não pode mudá-los.

**Decisão.** `ServicoDeMultas`, no pacote `extensao`, recebe uma `Biblioteca` e usa só as interfaces
do contrato (`listarEmprestimosDoUsuario`, `listarUsuarios`). A multa é calculada na hora da consulta:
dias entre a data prevista e a devolução (ou a data da consulta, se o livro ainda não voltou), vezes
o valor por dia. O dinheiro é `BigDecimal` com duas casas, nunca `double`.

**Alternativa rejeitada.** Guardar a multa no `Emprestimo` (um campo `multa` atualizado na devolução)
ou cobrá-la dentro de `devolver`. Um valor guardado à parte pode ficar diferente da lista real de
empréstimos (a mesma razão pela qual `Item` conta os exemplares em vez de guardar um total), e
mexer em `devolver` mudaria o comportamento do contrato e quebraria os testes de correção.

**Consequência.** A persistência não sabe que a multa existe, e ela volta certa depois de `carregar()`
(há teste). Em troca, não há "pagamento" nem histórico de multas: isso exigiria estado guardado, e
ficaria para uma extensão futura.

## Refatorações dos cheiros da Entrega 1 (Aula 10)

Cada linha é um passo pequeno, protegido pela suíte verde antes e depois. Serve de base para a
descrição dos PRs de refatoração (que nomeiam o cheiro e a refatoração aplicada).

| Cheiro | Onde estava | Refatoração | Onde está agora |
|---|---|---|---|
| Código duplicado | "localizar e lançar `RecursoNaoEncontradoException`" copiado em `buscarItem`, `adicionarExemplar` e `listarExemplares` | Extrair método | `RegistroDaBiblioteca.exigirItem`, `exigirExemplar`, `exigirUsuario`, usados por todas as operações |
| Código duplicado | `buscarPorTitulo` e `buscarPorCategoria` repetiriam o laço de filtrar e ordenar | Extrair método | `AcervoEmMemoria.filtrar(criterio)` |
| Código duplicado | "buscar no mapa ou declarar o arquivo corrompido" repetido ao carregar | Extrair método | `PersistenciaDaBiblioteca.existente(...)` |
| Método longo | `salvar()` com ~40 linhas montando cinco arquivos | Extrair método | `linhasDeItens`, `linhasDeExemplares`, `linhasDeUsuarios`, `linhasDeEmprestimos`, `linhasDeReservas` |
| Feature Envy | contar exemplares disponíveis dentro do serviço, lendo a lista do `Item` | Mover método | `Item.exemplaresDisponiveis()` |
| Feature Envy | montar `ItemView`, `UsuarioView` e `ExemplarView` no serviço, lendo os campos de outro objeto | Mover método | `Item.paraView()`, `Usuario.paraView()`, `Exemplar.paraView()` (como `Emprestimo` e `Reserva` já faziam) |
| Grupo de dados / listas paralelas | matrículas e nomes de usuários em listas indexadas pela mesma posição | Extrair classe | `Usuario`, em `Map<String, Usuario>` |
| Coleção vazada | `Item.getExemplares()` devolvia a lista interna | Encapsular coleção | cópia imutável (`List.copyOf`) em `Item`, `Usuario` e `FilaDeReservas` |
| Identidade mutável | `setCodigo` e `setTombo` públicos | Remover setter | identidade `final`, sem setters, validada no construtor |
| Nomes que não dizem nada | `c[0]` ... `c[4]` ao interpretar uma linha | Renomear / extrair variável | `codigo`, `titulo`, `tombo`, `matricula`... |