<t:page title="Learn and practice Python">
<section class="hero">
  <div>
    <h1>Learn Python by writing it.</h1>
    <p>${course.total} short lessons with runnable examples, ${exerciseCount} judged exercises from FizzBuzz to edit distance,
      and instructors who review your code.</p>
    <div class="row">
      <c:choose>
        <c:when test="${course.next != null}">
          <a class="btn primary" href="${course.next.url}">${course.done > 0 ? 'Continue: ' : 'Start: '}${h:e(course.next.title)}</a>
        </c:when>
        <c:otherwise><a class="btn primary" href="/problems?difficulty=Hard">Try a hard exercise</a></c:otherwise>
      </c:choose>
      <a class="btn" href="/problems">Browse exercises</a>
    </div>
    <p class="small faint" style="margin-top:14px">${studentCount} students · ${submissionCount} submissions judged</p>
  </div>
  <div class="hero-code" aria-hidden="true">
    <div class="bar-top"><span>two_sum.py</span><span>Python 3</span></div>
<pre><span class="kw">def</span> <span class="fn">two_sum</span>(nums, target):
    seen = {}
    <span class="kw">for</span> i, x <span class="kw">in</span> <span class="fn">enumerate</span>(nums):
        <span class="kw">if</span> target - x <span class="kw">in</span> seen:
            <span class="kw">return</span> seen[target - x], i
        seen[x] = i</pre>
    <div class="result"><span class="verdict ac">Accepted</span><span class="muted">6 / 6 tests</span><span class="muted">48 ms</span></div>
  </div>
</section>

<div class="side-layout">
  <div>
    <div class="section-title"><h2>The course</h2><a class="small" href="/learn">All topics</a></div>
    <div class="panel topic-grid" style="padding:6px; gap:2px">
      <c:forEach var="t" items="${course.topics}">
        <a class="topic-link" href="/learn/${t.topic.slug}">
          <span class="topic-num">${t.topic.number}</span>
          <span><strong>${h:e(t.topic.title)}</strong><br>
            <span class="small muted">${t.total} lessons<c:if test="${t.done > 0}"> · ${t.percent}% done</c:if></span></span>
        </a>
      </c:forEach>
    </div>

    <div class="section-title section"><h2>Exercises</h2><a class="small" href="/problems">All ${exerciseCount}</a></div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>Title</th><th>Difficulty</th><th class="num">Acceptance</th></tr></thead>
        <tbody>
        <c:forEach var="pr" items="${featured}">
          <c:set var="st" value="${acceptance[pr.id]}"/>
          <tr><td><a href="/problems/${pr.slug}">${h:e(pr.title)}</a></td>
            <td><span class="diff ${pr.difficulty.css}">${pr.difficulty.label}</span></td>
            <td class="num muted"><c:choose><c:when test="${st != null && st[0] > 0}">${h:pct(st[1], st[0])}%</c:when><c:otherwise>—</c:otherwise></c:choose></td></tr>
        </c:forEach>
        </tbody>
      </table>
    </div>
  </div>

  <aside class="stack">
    <c:if test="${not empty challenges}">
      <div class="panel">
        <div class="panel-head"><h3>Active challenges</h3><a class="small" href="/challenges">All</a></div>
        <ul class="list">
          <c:forEach var="ch" items="${challenges}">
            <li><a href="/challenges/${ch.id}">${h:e(ch.title)}</a><span class="small muted">ends ${h:date(ch.endAt)}</span></li>
          </c:forEach>
        </ul>
      </div>
    </c:if>
    <c:if test="${site.leaderboard_enabled}">
      <div class="panel">
        <div class="panel-head"><h3>Top students</h3><a class="small" href="/leaderboard">Leaderboard</a></div>
        <ul class="list">
          <c:forEach var="r" items="${leaders}">
            <li><span><span class="faint mono">${r.rank}</span>&nbsp; <a href="/u/${h:e(r.user.username)}">${h:e(r.user.displayName)}</a></span>
              <span class="small muted">${r.score} pts</span></li>
          </c:forEach>
          <c:if test="${empty leaders}"><li class="empty">No solves yet.</li></c:if>
        </ul>
      </div>
    </c:if>
    <div class="panel">
      <div class="panel-head"><h3>How it works</h3></div>
      <ol class="small" style="margin:0; padding:12px 14px 12px 32px; line-height:1.8">
        <li>Read a lesson and run its examples.</li>
        <li>Answer the quiz to complete it.</li>
        <li>Solve linked exercises; the judge runs hidden tests.</li>
        <li>Instructors grade and comment on your code.</li>
      </ol>
    </div>
  </aside>
</div>
</t:page>
