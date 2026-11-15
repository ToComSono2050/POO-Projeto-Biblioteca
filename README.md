# Biblioteca: acervo de livros

Programação Orientada a Objetos · 2026.2 · DCX / UFPB · Entregas 1 e 2

| Nome | Matrícula | Usuário no GitHub |
|---|---|---|
| [Samuel lima gomes] | [20240009760] | [samuka31] |
| [Kleyton Pedro da Cunha] | [20250114188] | [ToComSono2050] |

## Como executar

Precisa de **JDK 21** (Temurin) e Maven (ou o Maven do IntelliJ).

```bash
mvn -B verify
```

Quando dá certo, o final da saída mostra `Failures: 0, Errors: 0` e `BUILD SUCCESS`. A mesma
coisa roda no GitHub Actions a cada push.

## Mapa do projeto

    contrato/       CONGELADO: interfaces, visões, exceções e enums do contrato.
    Fabrica.java    devolve uma BibliotecaDaEquipe nova e vazia; não lê o disco.
    dominio/        Item, Exemplar, Reserva, FilaDeReservas, Usuario, Emprestimo, Validacao.
    servico/        BibliotecaDaEquipe (Facade), AcervoEmMemoria, UsuariosEmMemoria,
                    EmprestimosEmMemoria, RegistroDaBiblioteca, PersistenciaDaBiblioteca.
    persistencia/   ArquivoDeTabela, DiretorioDeDados, FalhaDePersistencia (não conhecem o domínio).
    extensao/       ServicoDeMultas e Multa (a extensão autoral).
    docs/           modelo.puml (e modelo.png) e decisoes.md.

## Onde ficam os dados

`salvar()` grava em `dados/` (ou no diretório da propriedade `-Dbiblioteca.dados=...`) cinco arquivos
de texto: `itens.txt`, `exemplares.txt`, `usuarios.txt`, `emprestimos.txt` e `reservas.txt`, uma linha
por registro, campos separados por `;`. O diretório é lido a cada chamada de `salvar()` e `carregar()`.
Arquivo ou diretório ausente equivale a biblioteca vazia; arquivo corrompido lança
`PersistenciaException` e não altera o estado em memória. Os arquivos gerados não são versionados
(`.gitignore`); o `dados/itens-exemplo.csv` do repositório nunca é tocado.

## Justificativa das coleções

- **Itens: `Map<String, Item>` por código.** A operação predominante é buscar item por código
  (`buscarItem`, a checagem de duplicidade em `cadastrarItem`, o localizar de `adicionarExemplar`).
  Com `List` cada uma seria uma varredura completa; com `Map` é acesso direto.
- **Tombos: `Map<String, Exemplar>`.** O tombo é único no acervo inteiro, então a verificação precisa
  ser feita sem depender de qual item o exemplar pertence.
- **Exemplares dentro do item: `List<Exemplar>`.** A operação é percorrer tudo (contar disponíveis,
  listar); a lista guarda a ordem de inserção e só sai do item como cópia.
- **Usuários: `Map<String, Usuario>` por matrícula.** Busca e unicidade por chave. As antigas listas
  paralelas de matrículas e nomes deram lugar à classe `Usuario`.
- **Empréstimos: `List<Emprestimo>` e `Map<String, Emprestimo>` por tombo (só os ativos).** A lista
  guarda a ordem de registro e é percorrida em `listarAtrasados`; o mapa deixa `devolver` achar o
  empréstimo ativo de um tombo sem varrer todos.
- **Reservas: `Map<String, Reserva>` por id e uma `FilaDeReservas` por item.** `cancelarReserva`
  recebe um id (chave única); a fila precisa de ordem de chegada, então é uma lista dentro de uma
  classe que só deixa entrar, sair e perguntar quem é o próximo.

As chaves são `String`, então não foi preciso implementar `equals`/`hashCode` em classes nossas.

## Relato do defeito

[Ajustem ao que realmente aconteceu.] No código inicial, `AcervoEmMemoria.localizar` comparava o
código do item com `==`, que compara referências, e não com `equals`. Com literais no código de
teste, as duas strings "L1" são o mesmo objeto e tudo funcionava; mas um código lido de arquivo ou
digitado é outro objeto com o mesmo conteúdo, e o sistema deixava cadastrar código duplicado e não
achava o item em `buscarItem`. Foi reproduzido cadastrando `new String("123")` duas vezes (o segundo
cadastro não lançava `RecursoDuplicadoException`) e encontrado ao ler `localizar`, que todos os outros
métodos usam. O teste que prova o defeito é o `DefeitoOcultoTest`, commitado antes da correção.

## Nossa extensão

**Acervo:** livros.

**Regra própria: multa por atraso.** Cada livro devolvido depois da data prevista, ou ainda emprestado
e já vencido na data da consulta, custa R$ 0,50 por dia de atraso. No próprio dia previsto ainda não
há atraso, igual à regra do contrato. A multa é *calculada* a cada consulta a partir das datas dos
empréstimos, e nunca guardada: assim ela não pode ficar diferente dos empréstimos.

**Como usar:**

```java
ServicoDeMultas multas = new ServicoDeMultas(biblioteca);          // R$ 0,50 por dia
LocalDate data = LocalDate.of(2026, 11, 21);

multas.totalDoUsuario("2026001", data);      // BigDecimal, por exemplo 2.50
multas.multasDoUsuario("2026001", data);     // uma Multa por empréstimo atrasado
multas.usuariosComMulta(data);               // quem deve: matrícula e total
new ServicoDeMultas(biblioteca, new BigDecimal("1.00"));           // outro valor por dia
```

**Onde está no código:** `extensao/ServicoDeMultas.java` e `extensao/Multa.java`; testes em
`MultaPorAtrasoTest`. Fica por fora do contrato: lê só pela interface `Biblioteca`, e não muda
`emprestar`, `devolver` nem o prazo de 14 dias.

## Decisões de design

Composição em vez de herança, as duas decisões GRASP, a Facade e as decisões que a especificação
deixou para a equipe estão em [docs/decisoes.md](docs/decisoes.md).

## Uso de IA

Veja [DECLARACAO-DE-USO-DE-IA.md](DECLARACAO-DE-USO-DE-IA.md).
