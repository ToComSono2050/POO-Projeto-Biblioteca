## Mapa do projeto

```
src/main/java/br/ufpb/dcx/poo/biblioteca/
├── contrato/   CONGELADO. Interfaces, views, exceções e enums do contrato.
├── Fabrica.java   Ponto de entrada; devolve uma BibliotecaDaEquipe nova a cada chamada.
├── dominio/    Regras dos objetos: Item, Exemplar, Usuario (e Validacao, uso interno).
└── servico/    Serviços em memória: AcervoEmMemoria, UsuariosEmMemoria,
                BibliotecaDaEquipe e os esqueletos de Empréstimos e Relatórios (Entregas 2 e 3).
src/test/java/…   Testes públicos, testes complementares e a regressão do defeito.
docs/modelo.puml  Diagrama UML do domínio (imagem gerada em docs/modelo.png).
```

## Como executar

Requer **JDK 21** (Temurin) e Maven (ou o Maven do IntelliJ).

```bash
mvn -B verify
```

Quando dá certo, o final da saída mostra `Failures: 0, Errors: 0` e `BUILD SUCCESS`.

## Justificativa das coleções

- **Itens: `Map<String, Item>` por código.** A operação predominante é buscar item por código
  (`buscarItem`, a checagem de duplicidade em `cadastrarItem` e o localizar de
  `adicionarExemplar`). Com `List` cada uma seria uma varredura completa; com `Map` é acesso direto.
- **Tombos: `Map<String, Exemplar>` no acervo.** O tombo é único no acervo inteiro, então a
  verificação precisa ser feita sem depender de qual item o exemplar pertence.
- **Exemplares dentro do item: `List<Exemplar>`.** A operação é percorrer tudo (contar
  disponíveis, listar); a lista guarda a ordem de inserção e nunca é exposta, só cópias.
- **Usuários: `Map<String, Usuario>` por matrícula.** Mesma razão do acervo: busca e unicidade
  por chave. As antigas listas paralelas de matrículas e nomes deram lugar à classe `Usuario`.

As chaves são `String`, então não foi preciso implementar `equals`/`hashCode` em classes nossas.

## Relato do defeito

No código inicial, `AcervoEmMemoria.localizar` comparava o código do item com `==`, que compara
referências, e não com `equals`. Com literais no código de teste, as duas strings "L1" são o mesmo
objeto e tudo funcionava; mas um código lido de arquivo ou digitado é outro objeto com o mesmo
conteúdo, e o sistema deixava cadastrar código duplicado e não achava o item em `buscarItem`.
Foi reproduzido cadastrando `new String("L1")` duas vezes (o segundo cadastro não lançava
`RecursoDuplicadoException`) e encontrado ao ler `localizar`, que todos os outros métodos usam.
O teste que prova o defeito está em `AcervoRegressaoTest` e foi commitado antes da correção.

## Nossa extensão

> A equipe precisa escrever aqui: qual acervo e qual regra de negócio própria.
