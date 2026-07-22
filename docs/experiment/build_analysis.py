import csv, json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).parent
rows = list(csv.DictReader((root / "experiment_results.csv").open(encoding="utf-8")))
mechanisms = ["NO_LOCK", "SYNCHRONIZED", "OPTIMISTIC", "FILE_LOCK"]
summary = []
for m in mechanisms:
    group = [r for r in rows if r["mechanism"] == m]
    avg = lambda key: sum(float(r[key]) for r in group) / len(group)
    item = {"mechanism": m, "avgSuccess": avg("success"),
            "avgDoublePayment": avg("doublePayment"), "avgWrongLeave": avg("wrongLeave"),
            "avgElapsedMs": avg("elapsedMs"), "avgTPS": avg("TPS")}
    item["doublePaymentRate"] = item["avgDoublePayment"] / item["avgSuccess"] * 100
    item["wrongLeaveRate"] = item["avgWrongLeave"] / 2000 * 100
    summary.append(item)
(root / "experiment_summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
with (root / "experiment_summary.csv").open("w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=summary[0].keys()); w.writeheader(); w.writerows(summary)

colors = ["#C94C4C", "#2F6B8A", "#36A269", "#7768AE"]
font = ImageFont.truetype("arial.ttf", 25); bold = ImageFont.truetype("arialbd.ttf", 34)
def bar_chart(filename, title, series, ymax):
    im=Image.new("RGB",(1500,850),"white"); d=ImageDraw.Draw(im)
    d.text((750,35),title,font=bold,fill="#17324D",anchor="ma")
    left,top,right,bottom=130,130,1420,700
    d.line((left,top,left,bottom,right,bottom),fill="#607080",width=3)
    group_w=(right-left)/4
    for si,(label,vals,color) in enumerate(series):
        bw=group_w/(len(series)+1)
        for i,v in enumerate(vals):
            x=left+i*group_w+35+si*bw; h=(v/ymax)*(bottom-top)
            d.rectangle((x,bottom-h,x+bw*.8,bottom),fill=color)
            d.text((x+bw*.4,bottom-h-12),f"{v:,.1f}",font=font,fill="#263238",anchor="ms")
    for i,m in enumerate(mechanisms): d.text((left+(i+.5)*group_w,bottom+28),m,font=font,fill="#263238",anchor="ma")
    for i,(label,_,color) in enumerate(series):
        x=420+i*330; d.rectangle((x,765,x+28,793),fill=color); d.text((x+42,780),label,font=font,fill="#263238",anchor="lm")
    im.save(root/filename)
bar_chart("throughput_comparison.png","Throughput trung bình — 20 threads × 3 lần",
          [("TPS",[s["avgTPS"] for s in summary],"#2F6B8A")],13000)
bar_chart("error_rates.png","Tỷ lệ lỗi hậu kiểm từ CSV",
          [("Double payment %",[s["doublePaymentRate"] for s in summary],"#D95F59"),
           ("Wrong leave %",[s["wrongLeaveRate"] for s in summary],"#E6A23C")],110)

im=Image.new("RGB",(1400,850),"white"); d=ImageDraw.Draw(im)
d.text((700,35),"Đánh đổi tính đúng đắn và hiệu năng",font=bold,fill="#17324D",anchor="ma")
left,top,right,bottom=140,120,1320,730; d.line((left,top,left,bottom,right,bottom),fill="#607080",width=3)
for s,c in zip(summary,colors):
    err=s["doublePaymentRate"]+s["wrongLeaveRate"]; x=left+s["avgTPS"]/13000*(right-left); y=bottom-err/190*(bottom-top)
    d.ellipse((x-14,y-14,x+14,y+14),fill=c); d.text((x+20,y-18),s["mechanism"],font=font,fill=c)
d.text((730,790),"Throughput (employee/giây) →",font=font,fill="#263238",anchor="ma")
d.text((35,420),"Tổng tỷ lệ lỗi (%)",font=font,fill="#263238")
im.save(root/"tradeoff_scatter.png")
