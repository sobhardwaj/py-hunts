class Matching {
  constructor(prefs, group_size = 4, iter_count = 2, final_iter_count = 4) {
    this.group_size = group_size;
    this.prefs = prefs;
    this.iter_count = iter_count;
    this.final_iter_count = final_iter_count;
    this.num_members = prefs.length;
    this.num_groups = Math.ceil(this.num_members / group_size);
    this.ungrouped = Array.from({ length: this.num_members }, (_, i) => i);
    this.unfilled = [];
    this.filled = [];

    const randomIndices = new Set();
    while (randomIndices.size < this.num_groups) {
      randomIndices.add(Math.floor(Math.random() * this.num_members));
    }

    for (const i of randomIndices) {
      this.unfilled.push(new Group(this, [i]));
      this.ungrouped.splice(this.ungrouped.indexOf(i), 1);
    }
  }

  static from_csv(file_path, r = 4) {
    // You'll need to implement file reading and parsing in JavaScript
    // and pass the preferences as a 2D array (prefs) to the constructor.
    const prefs = ...; // Parse CSV data
    return new Matching(prefs, r);
  }

  get_mem_pref_for_group(mem, grp) {
    let pref = 0;
    for (const i of grp) {
      pref += this.prefs[mem][i];
    }
    pref *= 1.0 / grp.length;
    return pref;
  }

  get_group_pref_for_mem(mem, grp) {
    let pref = 0;
    for (const i of grp) {
      pref += this.prefs[i][mem];
    }
    pref *= 1.0 / grp.length;
    return pref;
  }

  get_group_score(y) {
    if (y.length <= 1) {
      return 0;
    }
    let score = 0;
    for (const i of y) {
      for (const j of y) {
        if (i !== j) {
          score += this.prefs[i][j];
        }
      }
    }
    score *= 1.0 / (y.length ** 2 - y.length);
    return score;
  }

  get_net_score() {
    let score = 0;
    for (const i of this.filled) {
      score += this.get_group_score(i.members);
    }
    return score / this.num_groups;
  }

  solve() {
    while (this.ungrouped.length !== 0) {
      this.add_one_member();
    }

    this.filled.push(...this.unfilled);
    this.unfilled = [];
    this.optimize(true);
    const grps = this.filled.map((i) => i.members);
    return [this.get_net_score(), grps];
  }

  optimize(use_filled = true) {
    const grps = use_filled ? this.filled : this.unfilled;
    const iters = use_filled ? this.final_iter_count : this.iter_count;

    for (let a = 0; a < iters; a++) {
      for (const grp1 of grps) {
        for (const mem1 of grp1.members) {
          if (mem1 === -1) {
            break;
          }
          if (grp1 === grp2) {
            continue;
          }
          for (const grp2 of grps) {
            if (mem1 === -1) {
              break;
            }
            if (grp2 === grp1) {
              continue;
            }
            for (const mem2 of grp2.members) {
              if (mem1 === -1) {
                break;
              }
              if (mem2 === mem1) {
                continue;
              }
              const grp2mem1 = [...grp2.members];
              grp2mem1.splice(grp2mem1.indexOf(mem2), 1);
              grp2mem1.push(mem1);
              const grp1mem2 = [...grp1.members];
              grp1mem2.splice(grp1mem2.indexOf(mem1), 1);
              grp1mem2.push(mem2);

              const grp_one_new_score = this.get_group_score(grp1mem2);
              const grp_two_new_score = this.get_group_score(grp2mem1);

              if (grp_one_new_score + grp_two_new_score >
                this.get_group_score(grp1.members) +
                this.get_group_score(grp2.members)) {
                grp1.add_member(mem2);
                grp1.remove_member(mem1);
                grp2.add_member(mem1);
                grp2.remove_member(mem2);
                mem1 = -1;
              }
            }
          }
        }
      }
    }
  }

  add_one_member() {
    const proposed = Array.from({ length: this.ungrouped.length }, () => Array(this.unfilled.length).fill(false));
    const is_temp_grouped = new Array(this.ungrouped.length).fill(false);
    const temp_pref = Array.from({ length: this.ungrouped.length }, () => Array(this.unfilled.length).fill(0));
    const temp_pref_order = Array.from({ length: this.ungrouped.length }, () => Array(this.unfilled.length).fill(0));

    for (let i = 0; i < this.ungrouped.length; i++) {
      for (let j = 0; j < this.unfilled.length; j++) {
        temp_pref[i][j] = this.get_mem_pref_for_group(this.ungrouped[i], this.unfilled[j].members);
      }
    }

    for (let i = 0; i < this.ungrouped.length; i++) {
      temp_pref_order[i] = temp_pref[i].slice().sort((a, b) => b - a);
    }

    while (is_temp_grouped.filter((grouped) => !grouped).length !== 0) {
      for (let i = 0; i < this.ungrouped.length; i++) {
        if (is_temp_grouped[i]) {
          continue;
        }
        if (proposed[i].filter((p) => !p).length === 0) {
          is_temp_grouped[i] = true;
          continue;
        }
        for (const j of temp_pref_order[i]) {
          if (proposed[i][j]) {
            continue;
          }
          const grp = this.unfilled[j];
          proposed[i][j] = true;
          const pref = this.get_group_pref_for_mem(this.ungrouped[i], grp.members);
          if (pref > grp.tempScore) {
            if (grp.tempMember >= 0) {
              is_temp_grouped[this.ungrouped.indexOf(grp.tempMember)] = false;
            }
            grp.add_temp(this.ungrouped[i]);
            is_temp_grouped[i] = true;
            break;
          }
        }
      }
    }

    for (const grp of this.unfilled) {
      if (grp.tempMember < 0) {
        continue;
      }
      this.ungrouped.splice(this.ungrouped.indexOf(grp.tempMember), 1);
      grp.add_permanently();
    }

    this.optimize(false);

    for (const grp of this.unfilled) {
      if (grp.size() >= this.group_size || this.ungrouped.length === 0) {
        this.filled.push(grp);
      }
    }

    for (const grp of this.filled) {
      this.unfilled.splice(this.unfilled.indexOf(grp), 1);
    }
  }
}

class Group {
  constructor(game, members = []) {
    this.game = game;
    this.members = members;
    this.tempMember = -1;
    this.tempScore = -1;
  }

  add_member(x) {
    this.members.push(x);
  }

  remove_member(x) {
    const index = this.members.indexOf(x);
    if (index !== -1) {
      this.members.splice(index, 1);
    }
  }

  add_temp(x) {
    this.tempMember = x;
    this.tempScore = this.game.get_group_pref_for_mem(x, this.members);
    return this.tempScore;
  }

  add_permanently() {
    if (this.tempMember === -1) {
      return;
    }
    this.add_member(this.tempMember);
    this.tempMember = -1;
    this.tempScore = -1;
  }

  size() {
    return this.members.length;
  }
}
